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

import akka.actor._
import akka.util.Timeout
import com.typesafe.config.Config
import javax.inject.Inject
import io.lightcone.relayer.base._
import io.lightcone.lib._
import io.lightcone.relayer.data._
import io.lightcone.core._
import scala.concurrent._
import akka.event.LoggingReceive
import io.lightcone.ethereum.event.BlockEvent
import io.lightcone.persistence.DatabaseModule

//目标：需要恢复的以及初始化花费时间较长的
//定时keepalive, 定时给需要监控的发送req，确认各个shard等需要初始化的运行正常，否则会触发他们的启动恢复
object ChainReorganizationManagerActor extends DeployedAsSingleton {
  val name = "chain_reorg_manager"

  def start(
      implicit
      system: ActorSystem,
      config: Config,
      ec: ExecutionContext,
      timeProvider: TimeProvider,
      timeout: Timeout,
      actors: Lookup[ActorRef],
      dbModule: DatabaseModule,
      deployActorsIgnoringRoles: Boolean
    ): ActorRef = {
    startSingleton(Props(new ChainReorganizationManagerActor()))
  }
}

class ChainReorganizationManagerActor @Inject()(
    implicit
    val config: Config,
    val ec: ExecutionContext,
    val timeProvider: TimeProvider,
    val timeout: Timeout,
    val dbModule: DatabaseModule,
    val actors: Lookup[ActorRef])
    extends InitializationRetryActor
    with Stash
    with ActorLogging {

  val selfConfig = config.getConfig(ChainReorganizationManagerActor.name)

  val maxDepth = selfConfig.getInt("max-depth")
  val strictMode = selfConfig.getBoolean("strict-mode")
  val manager = new ChainReorganizationManagerImpl(maxDepth, strictMode)

  val metricName = s"chain_reorg_manager"
  val count = KamonSupport.counter(metricName)
  val timer = KamonSupport.timer(metricName)
  val gauge = KamonSupport.gauge(metricName)
  val histo = KamonSupport.histogram(metricName)

  def ready: Receive = LoggingReceive {
    case reorg.RecordOrderUpdateReq(block, orderIds) =>
      count.refine("label" -> "record_orders").increment()
      orderIds.foreach(manager.recordOrderUpdate(block, _))

    case reorg.RecordAccountUpdateReq(block, address, tokenAddress) =>
      count.refine("label" -> "record_accounts").increment()
      manager.recordAccountUpdate(block, address, tokenAddress)

    case event: BlockEvent =>
      count.refine("label" -> "block_event").increment()
      val impact = manager.reorganizedAt(event.blockNumber)
      gauge.refine("label" -> "impact_accounts").set(impact.accounts.size)
      histo.refine("label" -> "impact_orders").record(impact.orderIds.size)
      log.info(
        s"chain reorganized at ${event.blockNumber} with impact: $impact"
      )
      if (impact.orderIds.nonEmpty) {
        context
          .actorOf(
            Props(new RecoverOrdersActor()),
            s"reorg_restore_orders_${event.blockNumber}"
          ) ! impact
      }

      if (impact.accounts.nonEmpty) {
        context
          .actorOf(
            Props(new RecoverAccountsActor()),
            s"reorg_restore_accounts_${event.blockNumber}"
          ) ! impact
      }
  }

}
