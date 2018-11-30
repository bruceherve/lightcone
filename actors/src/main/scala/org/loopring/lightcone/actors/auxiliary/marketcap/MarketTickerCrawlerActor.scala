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

package org.loopring.lightcone.actors.auxiliary.marketcap

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Timers }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.loopring.lightcone.auxiliary.marketcap.crawler.MarketTickerCrawler
import org.loopring.lightcone.proto.auxiliary._

import scala.concurrent.duration._

class MarketTickerCrawlerActor(
    marketTickerServiceActor: ActorRef,
    tokenInfoServiceActor: ActorRef,
    crawler: MarketTickerCrawler
)(
    implicit
    val system: ActorSystem,
    val mat: ActorMaterializer
) extends Actor with Timers with ActorLogging {

  implicit val timeout = Timeout(5 seconds)
  implicit val ec = system.dispatcher

  override def preStart(): Unit = {
    //daily schedule market's ticker info
    timers.startPeriodicTimer("cronSyncMarketTicker", "syncMarketTicker", 600 seconds)
  }

  override def receive: Receive = {
    case _: String ⇒
      //load AllTokens
      val f = (tokenInfoServiceActor ? XGetTokenListReq()).mapTo[XGetTokenListRes]
      f.foreach {
        _.list.foreach { tokenInfo ⇒
          crawler.crawlMarketPairTicker(tokenInfo)
          Thread.sleep(50)
        }
      }
  }
}
