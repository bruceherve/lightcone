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

package org.loopring.lightcone.actors.entrypoint

import akka.actor._
import akka.util.Timeout
import org.loopring.lightcone.actors.base.Lookup
import org.loopring.lightcone.actors.core._
import org.loopring.lightcone.proto.actors._
import org.loopring.lightcone.proto.core._

import scala.concurrent.ExecutionContext

object EntryPointActor {
  val name = "entry-point"
}

class EntryPointActor()(
    implicit
    ec: ExecutionContext,
    timeout: Timeout,
    actors: Lookup[ActorRef]
)
  extends Actor with ActorLogging {

  //todo: 暂时测试，需要再对请求封装或者继续完善
  def receive: Receive = {
    case req: XSubmitOrderReq ⇒
      actors.get(AccountManagerActor.name) forward req

    case req: XGetOrderbookReq ⇒
      actors.get(OrderbookManagerActor.name) forward req

  }

}