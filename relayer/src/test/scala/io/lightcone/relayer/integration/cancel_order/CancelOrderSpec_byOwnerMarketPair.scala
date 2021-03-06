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

package io.lightcone.relayer.integration

import io.lightcone.core.ErrorCode._
import io.lightcone.core.{ErrorException, MarketPair}
import io.lightcone.core.OrderStatus.{
  STATUS_PENDING,
  STATUS_SOFT_CANCELLED_BY_USER
}
import io.lightcone.relayer._
import io.lightcone.relayer.data._
import io.lightcone.relayer.integration.AddedMatchers._
import org.scalatest._

class CancelOrderSpec_byOwnerMarketPair
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with CancelHelper
    with ValidateHelper
    with Matchers {

  feature("cancel orders of status=STATUS_PENDING") {
    scenario("3: cancel by owner-marketPair") {

      Given("an account with enough Balance")
      val anotherTokens = createAndSaveNewMarket(1.0, 1.0)
      val secondBaseToken = anotherTokens(0).getMetadata
      val secondQuoteToken = anotherTokens(1).getMetadata
      val secondMarket =
        MarketPair(secondBaseToken.address, secondQuoteToken.address)

      implicit val account = getUniqueAccount()
      val getAccountReq =
        GetAccount.Req(address = account.getAddress, allTokens = true)
      val accountInitRes = getAccountReq.expectUntil(
        check((res: GetAccount.Res) => res.accountBalance.nonEmpty)
      )
      val baseTokenBalance =
        accountInitRes.getAccountBalance.tokenBalanceMap(
          dynamicMarketPair.baseToken
        )
      val secondBaseTokenBalance =
        accountInitRes.getAccountBalance.tokenBalanceMap(secondMarket.baseToken)

      Then("submit an order of first market.")
      val order1 = createRawOrder(
        tokenS = dynamicMarketPair.baseToken,
        tokenB = dynamicMarketPair.quoteToken,
        tokenFee = dynamicMarketPair.baseToken
      )
      val submitRes1 = SubmitOrder
        .Req(Some(order1))
        .expect(check((res: SubmitOrder.Res) => res.success))
      info(s"the result of submit order is ${submitRes1.success}")

      Then("submit an order of second market.")
      val order2 =
        createRawOrder(
          tokenS = secondMarket.baseToken,
          tokenB = secondMarket.quoteToken,
          tokenFee = secondMarket.baseToken
        )
      val submitRes2 = SubmitOrder
        .Req(Some(order2))
        .expect(check((res: SubmitOrder.Res) => res.success))

      Then(
        s"cancel the orders of owner:${account.getAddress} and market:${dynamicMarketPair}."
      )
      val cancelByOwnerReq =
        CancelOrder.Req(
          owner = account.getAddress,
          marketPair = Some(dynamicMarketPair),
          status = STATUS_SOFT_CANCELLED_BY_USER,
          time = BigInt(timeProvider.getTimeSeconds())
        )
      val sig = generateCancelOrderSig(cancelByOwnerReq)
      val cancelRes = cancelByOwnerReq
        .withSig(sig)
        .expect(check { res: CancelOrder.Res =>
          res.status == cancelByOwnerReq.status
        })

      Then("check the cancel result.")
      val secondExpectedBalance = secondBaseTokenBalance.copy(
        availableBalance = secondBaseTokenBalance.availableBalance - order2.amountS - order2.getFeeParams.amountFee,
        availableAllowance = secondBaseTokenBalance.availableAllowance - order2.amountS - order2.getFeeParams.amountFee
      )
      defaultValidate(
        containsInGetOrders(
          STATUS_SOFT_CANCELLED_BY_USER,
          order1.hash
        ) and containsInGetOrders(
          STATUS_PENDING,
          order2.hash
        ),
        accountBalanceMatcher(dynamicMarketPair.baseToken, baseTokenBalance)
          and accountBalanceMatcher(
            secondMarket.baseToken,
            secondExpectedBalance
          ),
        Map(
          dynamicMarketPair -> (orderBookIsEmpty(),
          userFillsIsEmpty(),
          marketFillsIsEmpty()),
          secondMarket -> (not(orderBookIsEmpty()),
          userFillsIsEmpty(),
          marketFillsIsEmpty())
        )
      )

      Then("cancel this market again.")
      val cancelAnotherReq =
        CancelOrder.Req(
          owner = account.getAddress,
          marketPair = Some(dynamicMarketPair),
          status = STATUS_SOFT_CANCELLED_BY_USER,
          time = BigInt(timeProvider.getTimeSeconds())
        )
      val sig2 = generateCancelOrderSig(cancelAnotherReq)
      val cancelAnotherRes = cancelAnotherReq
        .withSig(sig2)
        .expect(check { res: ErrorException =>
          res.error.code == ERR_ORDER_NOT_EXIST
        })
      cancelAnotherRes.error.code should be(ERR_ORDER_NOT_EXIST)
    }
  }
}
