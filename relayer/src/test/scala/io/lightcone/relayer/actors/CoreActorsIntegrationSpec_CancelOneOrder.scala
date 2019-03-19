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

package io.lightcone.relayer.actors

import akka.pattern._
import io.lightcone.relayer.support._
import io.lightcone.relayer.validator._
import io.lightcone.relayer.data._
import io.lightcone.core._
import io.lightcone.lib.NumericConversion

import scala.concurrent.Await

class CoreActorsIntegrationSpec_CancelOneOrder
    extends CommonSpec
    with EthereumSupport
    with MetadataManagerSupport
    with OrderHandleSupport
    with MultiAccountManagerSupport
    with MarketManagerSupport
    with OrderbookManagerSupport
    with OrderGenerateSupport {

  import OrderStatus._

  "submiting one from OrderHandleActor" must {
    "get depth in OrderbookManagerActor" in {
      val rawOrder = createRawOrder()
      val submitF = actors.get(MultiAccountManagerActor.name) ? SubmitOrder.Req(
        Some(rawOrder)
      )
      val submitRes = Await.result(submitF, timeout.duration)
      info(s"submit res: ${submitRes}")

      Thread.sleep(1000)
      actors.get(OrderbookManagerActor.name) ! GetOrderbook.Req(
        0,
        100,
        Some(MarketPair(LRC_TOKEN.address, WETH_TOKEN.address))
      )

      expectMsgPF() {
        case GetOrderbook.Res(Some(orderbook)) =>
          info("----orderbook status after submitted an order: " + orderbook)
          orderbook.sells should not be empty
      }

      info("cancel this order now. ")

      val cancelReq = CancelOrder.Req(
        id = rawOrder.hash,
        owner = rawOrder.owner,
        time = Some(
          NumericConversion.toAmount(BigInt(timeProvider.getTimeSeconds()))
        )
      )

      val cancelResF = actors.get(MultiAccountManagerMessageValidator.name) ? cancelReq
        .withSig(generateCancelOrderSig(cancelReq)(accounts.head))

      val cancelRes = Await.result(cancelResF, timeout.duration)
      info(s"submit res: ${cancelRes}")

      Thread.sleep(1000)

      val getOrderFromDbF = dbModule.orderService.getOrder(rawOrder.hash)
      val getOrderFromDb =
        Await.result(getOrderFromDbF.mapTo[Option[RawOrder]], timeout.duration)

      getOrderFromDb map { o =>
        o.getState.status should be(STATUS_SOFT_CANCELLED_BY_USER)
      }

      actors.get(OrderbookManagerActor.name) ! GetOrderbook.Req(
        0,
        100,
        Some(MarketPair(LRC_TOKEN.address, WETH_TOKEN.address))
      )

      expectMsgPF() {
        case GetOrderbook.Res(Some(orderbook)) =>
          info("----orderbook status after cancel this order: " + orderbook)
          assert(orderbook.sells.isEmpty)
      }
    }

  }

}