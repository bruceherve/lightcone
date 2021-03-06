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

package io.lightcone.relayer.cmc

import io.lightcone.core._
import io.lightcone.persistence._
import io.lightcone.relayer.actors._
import io.lightcone.relayer.external._
import io.lightcone.relayer.support._
import io.lightcone.relayer.data.{GetMarkets, GetTokens}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CMCCrawlerSpec
    extends CommonSpec
    with JsonrpcSupport
    with HttpSupport
    with EthereumSupport
    with DatabaseModuleSupport
    with MetadataManagerSupport {

  val metadataManagerActor = actors.get(MetadataManagerActor.name)

  "cmc crawler" must {
    "sina currency rate" in {
      val r =
        Await.result(
          fiatExchangeRateFetcher
            .fetchExchangeRates()
            .mapTo[Seq[TokenTickerRecord]],
          5.second
        )
      r.nonEmpty should be(true)
    }

//    "exchangerate-api" in {
//      val f = for {
//        r <- syncCurrencyTicker()
//      } yield r
//      val q = Await.result(f.mapTo[Seq[TokenTickerRecord]], timeout.duration)
//    }

    "request cmc tickers in USD and persist (CMCCrawlerActor)" in {
      val f = for {
        tokenSymbolSlugs_ <- dbModule.cmcCrawlerConfigForTokenDal.getConfigs()
        tokenTickers <- getMockedCMCTickers(tokenSymbolSlugs_)
        currencyTickers <- fiatExchangeRateFetcher.fetchExchangeRates()
      } yield (tokenTickers, currencyTickers)
      val q1 = Await.result(
        f.mapTo[
          (
              Seq[TokenTickerRecord],
              Seq[TokenTickerRecord]
          )
        ],
        50.second
      )
      q1._1.nonEmpty should be(true)
    }

    "getTokens require [metadata]" in {
      val f1 = singleRequest(GetTokens.Req(true), "get_tokens")
      val res1 = Await.result(f1.mapTo[GetTokens.Res], timeout.duration)
      res1.tokens.nonEmpty should be(true)
      res1.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.isEmpty should be(true)
        t.ticker.isEmpty should be(true)
      }
    }

    "getTokens require [metadata] with token address" in {
      val f1 = singleRequest(
        GetTokens.Req(requireMetadata = true, tokens = Seq(LRC_TOKEN.address)),
        "get_tokens"
      )
      val res1 = Await.result(f1.mapTo[GetTokens.Res], timeout.duration)
      res1.tokens.length == 1 should be(true)
      val lrc = res1.tokens.head
      lrc.metadata.nonEmpty should be(true)
      lrc.info.isEmpty should be(true)
      lrc.ticker.isEmpty should be(true)
    }

    "getTokens require [metadata, info]" in {
      val f1 = singleRequest(GetTokens.Req(true, true), "get_tokens")
      val res1 = Await.result(f1.mapTo[GetTokens.Res], timeout.duration)
      res1.tokens.nonEmpty should be(true)
      res1.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.nonEmpty should be(true)
        t.ticker.isEmpty should be(true)
      }
    }

    "getTokens require [metadata, info, ticker]" in {
      val f1 = singleRequest(GetTokens.Req(true, true, true), "get_tokens")
      val res1 = Await.result(f1.mapTo[GetTokens.Res], timeout.duration)
      res1.tokens.nonEmpty should be(true)
      res1.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.nonEmpty should be(true)
        t.ticker.nonEmpty should be(true)
      }
    }

    "getTokens require [metadata, info, ticker] , with quote [ETH, RMB]" in {
      val f1 = singleRequest(GetTokens.Req(true, true, true), "get_tokens")
      val res1 = Await.result(f1.mapTo[GetTokens.Res], timeout.duration)
      res1.tokens.nonEmpty should be(true)
      res1.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.nonEmpty should be(true)
        t.ticker.nonEmpty should be(true)
      }
      val f2 = singleRequest(
        GetTokens.Req(true, true, true, "ETH"),
        "get_tokens"
      )
      val res2 = Await.result(f2.mapTo[GetTokens.Res], timeout.duration)
      res2.tokens.nonEmpty should be(true)
      res2.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.nonEmpty should be(true)
        t.ticker.nonEmpty should be(true)
      }

      val f3 = singleRequest(
        GetTokens.Req(true, true, true, "CNY"),
        "get_tokens"
      )
      val res3 = Await.result(f3.mapTo[GetTokens.Res], timeout.duration)
      res3.tokens.nonEmpty should be(true)
      res3.tokens.foreach { t =>
        t.metadata.nonEmpty should be(true)
        t.info.nonEmpty should be(true)
        t.ticker.nonEmpty should be(true)
      }

      val lrc1 = res1.tokens.find(_.getMetadata.symbol == LRC_TOKEN.symbol)
      lrc1.nonEmpty should be(true)
      val lrc2 = res2.tokens.find(_.getMetadata.symbol == LRC_TOKEN.symbol)
      lrc2.nonEmpty should be(true)
      val lrc3 = res3.tokens.find(_.getMetadata.symbol == LRC_TOKEN.symbol)
      lrc3.nonEmpty should be(true)
      lrc1 should not be lrc2
      lrc1 should not be lrc3
      lrc2 should not be lrc3
      val t1 = lrc1.get.getTicker
      val t2 = lrc2.get.getTicker
      val t3 = lrc3.get.getTicker
      t1.price should not be t2.price
      t1.price should not be t3.price
      t2.price should not be t3.price
      t1.volume24H should not be t2.volume24H
      t1.volume24H should not be t3.volume24H
      t2.volume24H should not be t3.volume24H
    }

    "getMarkets require [metadata]" in {
      val f1 = singleRequest(GetMarkets.Req(true), "get_markets")
      val res1 = Await.result(f1.mapTo[GetMarkets.Res], timeout.duration)
      res1.markets.nonEmpty should be(true)
      res1.markets.foreach { m =>
        m.metadata.nonEmpty should be(true)
        m.ticker.isEmpty should be(true)
      }
    }

    "getMarkets require [metadata] with market pair" in {
      val lrcWeth = MarketPair(LRC_TOKEN.address, WETH_TOKEN.address)
      val f1 = singleRequest(
        GetMarkets.Req(requireMetadata = true, marketPairs = Seq(lrcWeth)),
        "get_markets"
      )
      val res1 = Await.result(f1.mapTo[GetMarkets.Res], timeout.duration)
      res1.markets.length should be(1)
      val m = res1.markets.head
      m.metadata.nonEmpty should be(true)
      m.ticker.isEmpty should be(true)
    }

    "getMarkets require [metadata, ticker]" in {
      val f1 = singleRequest(GetMarkets.Req(true, true), "get_markets")
      val res1 = Await.result(f1.mapTo[GetMarkets.Res], timeout.duration)
      res1.markets.nonEmpty should be(true)
      res1.markets.foreach { m =>
        m.metadata.nonEmpty should be(true)
        m.ticker.nonEmpty should be(true)
        m.ticker.get.price > 0 should be(true)
      }
    }

    "getMarkets require [metadata, ticker], with quote [BTC, GBP]" in {
      val f1 = singleRequest(GetMarkets.Req(true, true), "get_markets")
      val res1 = Await.result(f1.mapTo[GetMarkets.Res], timeout.duration)
      res1.markets.nonEmpty should be(true)
      res1.markets.foreach { m =>
        m.metadata.nonEmpty should be(true)
        m.ticker.nonEmpty should be(true)
        m.ticker.get.price > 0 should be(true)
      }

      val f2 = singleRequest(
        GetMarkets.Req(true, true, false, "BTC"),
        "get_markets"
      )
      val res2 = Await.result(f2.mapTo[GetMarkets.Res], timeout.duration)
      res2.markets.nonEmpty should be(true)
      res2.markets.foreach { m =>
        m.metadata.nonEmpty should be(true)
        m.ticker.nonEmpty should be(true)
        m.ticker.get.price > 0 should be(true)
      }

      val f3 = singleRequest(
        GetMarkets.Req(true, true, false, "GBP"),
        "get_markets"
      )
      val res3 = Await.result(f3.mapTo[GetMarkets.Res], timeout.duration)
      res3.markets.nonEmpty should be(true)
      res3.markets.foreach { m =>
        m.metadata.nonEmpty should be(true)
        m.ticker.nonEmpty should be(true)
        m.ticker.get.price > 0 should be(true)
      }

      val lrcWethHash =
        MarketHash(MarketPair(LRC_TOKEN.address, WETH_TOKEN.address))
          .hashString()
      val lrcWeth1 = res1.markets.find(_.getMetadata.marketHash == lrcWethHash)
      lrcWeth1.nonEmpty should be(true)
      val lrcWeth2 = res2.markets.find(_.getMetadata.marketHash == lrcWethHash)
      lrcWeth2.nonEmpty should be(true)
      val lrcWeth3 = res3.markets.find(_.getMetadata.marketHash == lrcWethHash)
      lrcWeth3.nonEmpty should be(true)
      lrcWeth1 should not be lrcWeth2
      lrcWeth1 should not be lrcWeth3
      lrcWeth2 should not be lrcWeth3
      val t1 = lrcWeth1.get.getTicker
      val t2 = lrcWeth2.get.getTicker
      val t3 = lrcWeth3.get.getTicker
      t1.exchangeRate should be(t2.exchangeRate)
      t1.exchangeRate should be(t3.exchangeRate)
      t2.exchangeRate should be(t3.exchangeRate)
      t1.price should not be t2.price
      t1.price should not be t3.price
      t2.price should not be t3.price
      t1.volume24H should not be t2.volume24H
      t1.volume24H should not be t3.volume24H
      t2.volume24H should not be t3.volume24H
    }
  }

  private def getMockedCMCTickers(
      symbolSlugs: Seq[CMCCrawlerConfigForToken]
    ) = {
    import scala.io.Source
    val fileContents = Source.fromResource("cmc.data").getLines.mkString

    val res = parser.fromJsonString[CMCResponse](fileContents)
    res.status match {
      case Some(r) if r.errorCode == 0 =>
        Future.successful(
          externalTickerFetcher.filterSupportTickers(symbolSlugs, res.data)
        )
      case Some(r) if r.errorCode != 0 =>
        log.error(
          s"Failed request CMC, code:[${r.errorCode}] msg:[${r.errorMessage}]"
        )
        Future.successful(Seq.empty)
      case m =>
        log.error(s"Failed request CMC, return:[$m]")
        Future.successful(Seq.empty)
    }
  }

  private def mockSinaCurrencyError(): Future[Seq[TokenTickerRecord]] = {
    if (true) {
      Future {
        throw ErrorException(ErrorCode.ERR_INTERNAL_UNKNOWN, "mock error")
      }
    } else {
      Future.successful(Seq.empty)
    }
  }

  private def syncCurrencyTicker() = {
    mockSinaCurrencyError() recoverWith {
      case e: Exception => {
        exchangeRateAPIFetcher.fetchExchangeRates()
      }
    }
  }
}
