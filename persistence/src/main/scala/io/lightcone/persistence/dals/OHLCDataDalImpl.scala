/*
 * Copyright 2018 Loopring Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lightcone.persistence.dals

import io.lightcone.core.ErrorCode._
import scala.concurrent.{Await, ExecutionContext, Future}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.google.protobuf.any.Any
import io.lightcone.core.ErrorCode
import io.lightcone.ethereum.event.BlockEvent
import io.lightcone.ethereum.persistence._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcProfile}
import slick.basic.DatabaseConfig
import slick.lifted.TableQuery
import scala.concurrent.duration._

class OHLCDataDalImpl @Inject()(
    implicit
    val ec: ExecutionContext,
    @Named("dbconfig-dal-ohlc-data") val dbConfig: DatabaseConfig[JdbcProfile])
    extends OHLCDataDal {

  val query = TableQuery[OHLCDataTable]

  override def createTable() = {
    implicit val getOHLCResult = GetResult[Any](r => Any(r.nextString()))
    val sqlCreateExtention =
      sql"""CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE""".as[Any]
    try {
      Await.result(db.run(sqlCreateExtention), 10.second)
    } catch {
      case e: Exception =>
        logger.error(
          s"Failed to create timescaledb extension: ${e.getMessage}, cause:${e.getCause}"
        )
        System.exit(0)
    }

    super.createTable()

    val sqlCreateHypertable =
      sql"""SELECT CREATE_HYPERTABLE('"T_OHLC_DATA"', 'time', CHUNK_TIME_INTERVAL => 604800)"""
        .as[Any]
    try {
      Await.result(db.run(sqlCreateHypertable), 10.second)
    } catch {
      case e: Exception if e.getMessage.contains("already a hypertable") =>
        logger.info(e.getMessage)
      case e: Exception =>
        logger.error("Failed to create hypertable: " + e.getMessage)
        System.exit(0)
    }
  }

  def saveData(
      record: OHLCRawData
    ): Future[(ErrorCode, Option[OHLCRawData])] = {
    for {
      result <- db.run(query.insertOrUpdate(record))
    } yield {
      if (result == 1) {
        (ERR_NONE, Some(record))
      } else {
        (ERR_PERSISTENCE_INTERNAL, None)
      }
    }
  }

  def getOHLCData(
      marketHash: String,
      interval: Long,
      beginTime: Long,
      endTime: Long
    ): Future[Seq[Seq[Double]]] = {

    implicit val result = GetResult[Seq[Double]](
      r =>
        Seq(
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble
        )
    )

    val sql = sql"""select
        TIME_BUCKET($interval, time) AS starting_point,
        SUM(base_amount) AS base_amount_sum,
        SUM(quote_amount) AS quote_amount_sum,
        FIRST(price, time) AS opening_price,
        LAST(price, time) AS closing_price,
        MAX(price) AS highest_price,
        MIN(price) AS lowest_price
        FROM "T_OHLC_DATA" t
        WHERE market_hash = ${marketHash}
        AND time > ${beginTime} AND
        time < ${endTime} GROUP BY starting_point
        ORDER BY starting_point DESC
        """.as[Seq[Double]]

    db.run(sql)
  }

  // TODO(yangli):根据当前讨论，先保留查询接口，目前前端只显示relayer的最新价格，看将来需求是否启用这个接口
  def getRecentOHLCData(
      marketHash: String,
      beginTime: Long
    ): Future[Seq[Seq[Double]]] = {

    implicit val result = GetResult[Seq[Double]](
      r =>
        Seq(
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble,
          r.nextDouble
        )
    )
    val sql = sql"""select
        SUM(base_amount) AS base_amount_sum,
        SUM(quote_amount) AS quote_amount_sum,
        FIRST(price, time) AS opening_price,
        LAST(price, time) AS closing_price,
        MAX(price) AS highest_price,
        MIN(price) AS lowest_price
        FROM "T_OHLC_DATA" t
        WHERE market_hash = ${marketHash}
        AND time > ${beginTime}
        """.as[Seq[Double]]

    db.run(sql)
  }

  def cleanDataForReorg(req: BlockEvent): Future[Int] = db.run(
    query
      .filter(_.blockHeight >= req.blockNumber)
      .delete
  )
}
