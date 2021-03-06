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

import io.lightcone.persistence.base._
import io.lightcone.core._
import scala.concurrent._

trait TokenMetadataDal extends BaseDalImpl[TokenMetadataTable, TokenMetadata] {

  def saveTokenMetadata(tokenMetadata: TokenMetadata): Future[ErrorCode]

  def saveTokenMetadatas(
      tokenMetadatas: Seq[TokenMetadata]
    ): Future[Seq[String]]

  def updateTokenMetadata(tokenMetadata: TokenMetadata): Future[ErrorCode]

  def getTokenMetadatas(addresses: Seq[String]): Future[Seq[TokenMetadata]]

  def getTokenMetadatas(): Future[Seq[TokenMetadata]]

  def updateBurnRate(
      token: String,
      burnRateForMarket: Double,
      burnRateForP2P: Double
    ): Future[ErrorCode]

  def invalidateTokenMetadata(address: String): Future[ErrorCode]
}
