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

package org.loopring.lightcone.auxiliary.marketcap.crawler

import java.text.SimpleDateFormat

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.loopring.lightcone.auxiliary.marketcap.SeqTpro
import org.loopring.lightcone.auxiliary.marketcap.util.HttpConnector
import org.loopring.lightcone.proto.auxiliary._
import scalapb.json4s.Parser

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class TokenTickerCrawlerImpl(tokenTickerServiceActor: ActorRef)(
    implicit
    val system: ActorSystem,
    val mat: ActorMaterializer
) extends HttpConnector with TokenTickerCrawler {

  val config = system.settings.config
  val connection = https(config.getString("cmc-config.prefix_url"))
  val limitSize = config.getString("cmc-config.limitSize").toInt
  val appKey = config.getString("cmc-config.api_key")

  //val convertCurrency = config.getString("cmc-config.convertCurrency")

  val rawHeader = RawHeader(config.getString("cmc-config.header"), appKey)
  val parser = new Parser(preservingProtoFieldNames = true) //protobuf 序列化为json不使用驼峰命名
  // 初始化交易市场
  lazy val markets = Seq("LRC", "ETH", "TUSD", "USDT")

  def crawlTokenTickers(): Seq[XCMCTickerData] = {
    val futureResult = for {
      // TODO(xiaolu) 成为会员后替换
      // usdTickers ← getTokenTickers("USD,CNY")
      usdTickers ← getTokenTickers("USD")
      marketTickers = getMarketUSDQuote(usdTickers)
    } yield {
      for {
        usdTicker ← usdTickers
        usdQuote = usdTicker.quote.get("USD") //先找到每个token的usd quote
        _ = require(usdQuote.isDefined, s"can not found ${usdTicker.symbol} quote for USD")
        priceQuote = usdQuote.get
      } yield {
        val quoteMap = marketTickers.foldLeft(usdTicker.quote) { (map, marketQuote) ⇒
          //添加市场代币的Quote ("LRC", "ETH", "TUSD", "USDT")
          map + (marketQuote._1 → convertQuote(priceQuote, marketQuote._2))
        }
        //更新token的quote属性
        usdTicker.copy(quote = quoteMap)
      }
    }

    Await.result(futureResult, 10 seconds)
  }

  //获取cmc's token对锚定币convertCurrency的价格
  def getTokenTickers(convertCurrency: String): Future[Seq[XCMCTickerData]] = {
    val uri = s"/v1/cryptocurrency/listings/latest?start=1&limit=${limitSize}&convert=${convertCurrency}"
    get(HttpRequest(uri = uri, method = HttpMethods.GET).withHeaders(rawHeader)) {
      case HttpResponse(StatusCodes.OK, _, entity, _) ⇒
        entity.dataBytes.map(_.utf8String).runReduce(_ + _).map(parser.fromJsonString[XTickerDataInfo]).map(_.data)

      case _ ⇒
        Future.successful(Seq[XCMCTickerData]())
    }
  }

  //找到市场代币对USD的priceQuote
  def getMarketUSDQuote(tickers: Seq[XCMCTickerData]): Seq[(String, XQuote)] = {
    markets.map { s ⇒
      val priceQuote = tickers.find(_.symbol == s).flatMap(_.quote.get("USD"))
      require(priceQuote.isDefined, s"can not found ${s} / USD")
      (s, priceQuote.get)
    }
  }

  //锚定市场币的priceQuote换算
  def convertQuote(tokenQuote: XQuote, marketQuote: XQuote): XQuote = {
    val price = toDouble(BigDecimal(tokenQuote.price / marketQuote.price))
    val volume_24h = toDouble(BigDecimal(tokenQuote.volume24H / tokenQuote.price) * price)
    val market_cap = toDouble(BigDecimal(tokenQuote.marketCap / tokenQuote.price) * price)
    val percent_change_1h = BigDecimal(1 + tokenQuote.percentChange1H) / BigDecimal(1 + marketQuote.percentChange1H) - 1
    val percent_change_24h = BigDecimal(1 + tokenQuote.percentChange24H) / BigDecimal(1 + marketQuote.percentChange24H) - 1
    val percent_change_7d = BigDecimal(1 + tokenQuote.percentChange7D) / BigDecimal(1 + marketQuote.percentChange7D) - 1
    val last_updated = marketQuote.lastUpdated
    XQuote(price, volume_24h, toDouble(percent_change_1h), toDouble(percent_change_24h), toDouble(percent_change_7d), toDouble(market_cap), last_updated)
  }

  def convertTO(tickers: Seq[XCMCTickerData]): SeqTpro = {
    val f = tickers.flatMap {
      ticker ⇒
        val id = ticker.id
        val name = ticker.name
        val symbol = ticker.symbol
        val websiteSlug = ticker.slug
        val rank = ticker.cmcRank
        val circulatingSupply = ticker.circulatingSupply
        val totalSupply = ticker.totalSupply
        val maxSupply = ticker.maxSupply

        ticker.quote.map {
          priceQuote ⇒
            val market = priceQuote._1
            val quote = priceQuote._2
            val price = quote.price
            val volume24h = quote.volume24H
            val marketCap = quote.marketCap
            val percentChange1h = quote.percentChange1H
            val percentChange24h = quote.percentChange24H
            val percentChange7d = quote.percentChange7D
            val utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z")
            val lastUpdated = utcFormat.parse(quote.lastUpdated.replace("Z", " UTC")).getTime / 1000
            XTokenTickerInfo(id, name, symbol, websiteSlug, market, rank, circulatingSupply, totalSupply, maxSupply,
              price, volume24h, marketCap, percentChange1h, percentChange24h, percentChange7d, lastUpdated)
        }
    }
    SeqTpro(f)
  }

  def toDouble: PartialFunction[BigDecimal, Double] = {
    case s: BigDecimal ⇒ scala.util.Try(s.setScale(8, BigDecimal.RoundingMode.HALF_UP).toDouble).toOption.getOrElse(0)
  }

}