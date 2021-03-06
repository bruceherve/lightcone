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

package io.lightcone.core

class OrderbookAggregatorImpl(
    priceDecimals: Int,
    precisionForAmount: Int,
    precisionForTotal: Int
  )(
    implicit
    marketPair: MarketPair,
    metadataManager: MetadataManager)
    extends OrderbookAggregator {

  private val sells = new OrderbookSide.Sells(
    priceDecimals,
    0,
    precisionForAmount,
    precisionForTotal,
    true
  )
  private val buys = new OrderbookSide.Buys(
    priceDecimals,
    0,
    precisionForAmount,
    precisionForTotal,
    true
  )
  private val lastPrice: Double = 0

  def getOrderbookInternalUpdate() =
    Orderbook.InternalUpdate(sells.takeUpdatedSlots, buys.takeUpdatedSlots)

  def getOrderbookSlots(num: Int) =
    Orderbook.InternalUpdate(
      sells.getSlots(num, None),
      buys.getSlots(num, None)
    )

  def addOrder(order: Matchable) =
    adjustAmount(
      order.isSell,
      true,
      order.price,
      order.matchableBaseAmount,
      order.matchableQuoteAmount
    )

  def deleteOrder(order: Matchable) =
    adjustAmount(
      order.isSell,
      false,
      order.price,
      order.matchableBaseAmount,
      order.matchableQuoteAmount
    )

  def adjustAmount(
      isSell: Boolean,
      increase: Boolean,
      price: Double,
      amount: Double,
      total: Double
    ) = {
    if (price > 0 && amount > 0 && total > 0) {
      val side = if (isSell) sells else buys
      if (increase) side.increase(price, amount, total)
      else side.decrease(price, amount, total)
    }
  }

  def reset(): Unit = {
    sells.reset()
    buys.reset()
  }
}
