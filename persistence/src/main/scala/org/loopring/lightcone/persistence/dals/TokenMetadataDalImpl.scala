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

package org.loopring.lightcone.persistence.dals

import com.google.inject.Inject
import com.google.inject.name.Named
import org.loopring.lightcone.persistence.base._
import org.loopring.lightcone.persistence.tables._
import org.loopring.lightcone.proto.ErrorCode._
import org.loopring.lightcone.proto._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.JdbcProfile
import slick.basic._
import scala.concurrent._
import org.slf4s.Logging
import com.google.inject.Inject

class TokenMetadataDalImpl @Inject()(
    implicit
    val ec: ExecutionContext,
    @Named("dbconfig-dal-token-metadata") val dbConfig: DatabaseConfig[
      JdbcProfile
    ])
    extends TokenMetadataDal
    with Logging {

  val query = TableQuery[TokenMetadataTable]

  private var tokens: Seq[TokenMetadata] = Nil

  def getTokens(reloadFromDatabase: Boolean = false) = {
    if (reloadFromDatabase || tokens.isEmpty) {
      db.run(query.take(Int.MaxValue).result).map { tokens_ =>
        tokens = tokens_
        log.info(
          s"token metadata retrieved>> ${tokens.mkString("\n", "\n", "\n")}"
        )
        tokens
      }
    } else {
      Future.successful(tokens)
    }
  }

  def updateBurnRate(
      token: String,
      burnRate: Double
    ): Future[ErrorCode] =
    for {
      result <- db.run(
        query
          .filter(_.address === token)
          .map(c => c.burnRate)
          .update(burnRate)
      )
    } yield {
      if (result >= 1) ERR_NONE
      else ERR_PERSISTENCE_UPDATE_FAILED
    }
}