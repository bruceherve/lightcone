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

package org.loopring.lightcone.persistence.service

import com.google.inject.Inject
import com.google.inject.name.Named
import org.loopring.lightcone.persistence.dals.{CutoffDal, CutoffDalImpl}
import org.loopring.lightcone.proto.{XCutoff, XErrorCode}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

class CutoffServiceImpl @Inject()(
    implicit
    val dbConfig: DatabaseConfig[JdbcProfile],
    @Named("db-execution-context") val ec: ExecutionContext)
    extends CutoffService {
  val cutoffDal: CutoffDal = new CutoffDalImpl()

  def saveCutoff(cutoff: XCutoff): Future[XErrorCode] =
    cutoffDal.saveCutoff(cutoff)

  def hasCutoff(
      orderBroker: Option[String],
      orderOwner: String,
      orderTradingPair: String,
      time: Long
    ): Future[Boolean] =
    cutoffDal.hasCutoff(orderBroker, orderOwner, orderTradingPair, time)

  def obsolete(height: Long): Future[Unit] = cutoffDal.obsolete(height)

}
