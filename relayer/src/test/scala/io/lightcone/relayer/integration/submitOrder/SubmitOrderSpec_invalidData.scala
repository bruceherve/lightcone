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

package io.lightcone.relayer.integration.submitOrder

import io.lightcone.core.ErrorCode.ERR_ORDER_VALIDATION_INVALID_SIG
import io.lightcone.core.ErrorException
import io.lightcone.relayer.data.SubmitOrder
import io.lightcone.relayer.getUniqueAccount
import io.lightcone.relayer.integration.AddedMatchers.check
import io.lightcone.relayer.integration._
import org.scalatest._

class SubmitOrderSpec_invalidData
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with Matchers {

  feature("submit order") {
    scenario("invalid data of order") {
      implicit val account = getUniqueAccount()
      Given(
        s"an new account with enough balance and enough allowance: ${account.getAddress}"
      )

      When("submit an order that order sig is invalid")
      try {
        val order1 = createRawOrder()
        SubmitOrder
          .Req(
            Some(
              order1.copy(
                params = order1.params
                  .map(p => p.copy(sig = p.sig.replaceAll("f", "e")))
              )
            )
          )
          .expect(
            check(
              (err: ErrorException) =>
                err.error.code == ERR_ORDER_VALIDATION_INVALID_SIG
            )
          )
      } catch {
        case e: ErrorException =>
      }

      Then("the error code of submit order is ERR_ORDER_VALIDATION_INVALID_SIG")
      When("submit an order that order owner is invalid")
      try {
        val order2 = createRawOrder()
        SubmitOrder
          .Req(
            Some(
              order2.copy(
                owner = getUniqueAccount().getAddress
              )
            )
          )
          .expect(
            check(
              (err: ErrorException) =>
                err.error.code == ERR_ORDER_VALIDATION_INVALID_SIG
            )
          )
      } catch {
        case e: ErrorException =>
      }

      Then("the error code of submit order is ERR_ORDER_VALIDATION_INVALID_SIG")

      val order2 = createRawOrder(amountS = "20".zeros(18))
      SubmitOrder
        .Req(
          Some(
            order2.copy(
              params = order2.params
                .map(
                  p => p.copy(dualAuthSig = p.sig)
                )
            )
          )
        )
        .expect(
          check(
            (err: SubmitOrder.Res) => {
              println(err)
              true
            }
          )
        )

    }
  }
}
