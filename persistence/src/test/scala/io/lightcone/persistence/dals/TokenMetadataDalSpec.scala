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

import io.lightcone.core._
import scala.concurrent.Await
import scala.concurrent.duration._

class TokenMetadataDalSpec extends DalSpec[TokenMetadataDal] {
  def getDal = new TokenMetadataDalImpl()

  "save tokens config" must "save some token configs" in {
    info("save 3 token configs")
    val lrcAddress = "0x111"
    val tokens1 = Seq(
      TokenMetadata(
        `type` = TokenMetadata.Type.TOKEN_TYPE_ERC20,
        status = TokenMetadata.Status.VALID,
        symbol = "T1",
        name = "T1 Token",
        address = lrcAddress,
        unit = "T1",
        decimals = 18,
        precision = 6,
        burnRate = Some(BurnRate(0.1, 0.1))
      ),
      TokenMetadata(
        `type` = TokenMetadata.Type.TOKEN_TYPE_ERC20,
        status = TokenMetadata.Status.VALID,
        symbol = "T2",
        name = "T2 Token",
        address = "0x222",
        unit = "T2",
        decimals = 18,
        precision = 6,
        burnRate = Some(BurnRate(0.2, 0.2))
      ),
      TokenMetadata(
        `type` = TokenMetadata.Type.TOKEN_TYPE_ERC20,
        status = TokenMetadata.Status.VALID,
        symbol = "T3",
        name = "T3 Token",
        address = "0x333",
        unit = "T3",
        decimals = 18,
        precision = 6,
        burnRate = Some(BurnRate(0.3, 0.3))
      )
    )
    val r1 = dal.saveTokenMetadatas(tokens1)
    val res1 = Await.result(r1.mapTo[Seq[String]], 5.second)
    assert(res1.length == tokens1.length)

    info("query the token configs just saved")
    val r2 = dal.getTokenMetadatas(tokens1.map(_.address))
    val res2 = Await.result(r2.mapTo[Seq[TokenMetadata]], 5.second)
    assert(res2.length == tokens1.length)
    val lrc = res2.find(_.symbol == "T1").getOrElse(TokenMetadata())
    assert(
      lrc.`type` == TokenMetadata.Type.TOKEN_TYPE_ERC20 &&
        lrc.status == TokenMetadata.Status.VALID &&
        lrc.symbol == "T1" &&
        lrc.name == "T1 Token" &&
        lrc.address == lrcAddress &&
        lrc.unit == "T1" &&
        lrc.decimals == 18 &&
        lrc.precision == 6 &&
        lrc.burnRate.contains(BurnRate(0.1, 0.1))
    )

    info("duplicate token address save should return error")
    val token3 = lrc.copy(precision = 8)
    val r3 = dal.saveTokenMetadata(token3)
    val res3 = Await.result(r3.mapTo[ErrorCode], 5.second)
    assert(res3 == ErrorCode.ERR_PERSISTENCE_DUPLICATE_INSERT)
    val r4 = dal.getTokenMetadatas(Seq(token3.address))
    val res4 = Await.result(r4.mapTo[Seq[TokenMetadata]], 5.second)
    assert(res4.length == 1)
    val lrc1 = res4.find(_.symbol == "T1")
    assert(lrc1.nonEmpty)

    info(
      "should not save token with too long address :0xBe4C1cb10C2Be76798c4186ADbbC34356b358b521"
    )
    val r5 = dal.saveTokenMetadata(
      lrc.copy(address = "0xBe4C1cb10C2Be76798c4186ADbbC34356b358b521")
    )
    val res5 = Await.result(r5.mapTo[ErrorCode], 5.second)
    assert(res5 == ErrorCode.ERR_PERSISTENCE_INTERNAL)
    val r6 = dal.getTokenMetadatas(Seq(lrc.address))
    val res6 = Await.result(r6.mapTo[Seq[TokenMetadata]], 5.second)
    val lrc2 = res4.find(_.symbol == "T1")
    assert(lrc2.nonEmpty && lrc2.get.address == lrcAddress)

    info("update LRC's burn rate")
    val r7 = dal.updateBurnRate(lrcAddress, 0.5, 0.6)
    val res7 = Await.result(r7.mapTo[ErrorCode], 5.second)
    assert(res7 == ErrorCode.ERR_NONE)
    val r8 = dal.getTokenMetadatas(Seq(lrcAddress))
    val res8 = Await.result(r8.mapTo[Seq[TokenMetadata]], 5.second)
    val lrc3 = res8.find(_.symbol == "T1")
    lrc3.nonEmpty should be(true)
    lrc3.get.address should be(lrcAddress)
    lrc3.get.burnRate.contains(BurnRate(0.5, 0.6)) should be(true)

    info(
      "update T2's type, status, symbol, name, unit, decimal, website, precision, burn rate, usd price"
    )
    val bnb = res2.find(_.symbol == "T2").getOrElse(TokenMetadata())
    assert(
      bnb.`type` == TokenMetadata.Type.TOKEN_TYPE_ERC20 &&
        bnb.status == TokenMetadata.Status.VALID &&
        bnb.symbol == "T2" &&
        bnb.name == "T2 Token" &&
        bnb.address == "0x222" &&
        bnb.unit == "T2" &&
        bnb.decimals == 18 &&
        bnb.precision == 6 &&
        bnb.burnRate.contains(BurnRate(0.2, 0.2))
    )
    val r9 = dal.updateTokenMetadata(
      bnb.copy(
        `type` = TokenMetadata.Type.TOKEN_TYPE_ERC1400,
        status = TokenMetadata.Status.INVALID,
        symbol = "T2_",
        name = "T2_ Token",
        unit = "T2_",
        decimals = 12,
        precision = 8,
        burnRate = Some(BurnRate(0.5, 0.6))
      )
    )
    val res9 = Await.result(r9.mapTo[ErrorCode], 5.second)
    assert(res9 == ErrorCode.ERR_NONE)
    val r10 = dal.getTokenMetadatas(Seq(bnb.address))
    val res10 = Await.result(r10.mapTo[Seq[TokenMetadata]], 5.second)
    val bnb1 = res10.find(_.symbol == "T2_").getOrElse(TokenMetadata())
    assert(
      bnb1.`type` == TokenMetadata.Type.TOKEN_TYPE_ERC1400 &&
        bnb1.status == TokenMetadata.Status.INVALID &&
        bnb1.symbol == "T2_" &&
        bnb1.name == "T2_ Token" &&
        bnb1.unit == "T2_" &&
        bnb1.address == "0x222" &&
        bnb1.decimals == 12 &&
        bnb1.precision == 8 &&
        bnb1.burnRate.contains(BurnRate(0.5, 0.6))
    )
  }
}
