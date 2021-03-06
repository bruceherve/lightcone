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

package io.lightcone.relayer.validator

import com.typesafe.config.Config
import io.lightcone.relayer.data._
import io.lightcone.core.MetadataManager
import io.lightcone.relayer.data._
import io.lightcone.core._
import scala.concurrent._

// Owner: Hongyu
object OrderbookManagerMessageValidator {
  val name = "orderbook_manager_validator"
}

final class OrderbookManagerMessageValidator(
  )(
    implicit
    val config: Config,
    ec: ExecutionContext,
    metadataManager: MetadataManager)
    extends MessageValidator {

  import MarketMetadata.Status._

  // Throws exception if validation fails.
  def validate = {
    case msg @ GetOrderbook.Req(_, _, Some(marketPair)) =>
      Future {
        metadataManager.assertMarketStatus(marketPair, ACTIVE, READONLY)
        msg.copy(marketPair = Some(marketPair.normalize))
      }
  }
}
