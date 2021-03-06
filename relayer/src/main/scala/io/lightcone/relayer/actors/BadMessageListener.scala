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

// Owner: Daniel
object BadMessageListener {
  val name = "bad_message"

  def start(implicit system: ActorSystem) = {
    val actor =
      system.actorOf(Props[BadMessageListener], BadMessageListener.name)
    system.eventStream.subscribe(actor, classOf[UnhandledMessage])
    system.eventStream.subscribe(actor, classOf[DeadLetter])
    actor
  }
}

class BadMessageListener extends Actor with ActorLogging {

  def receive = {
    case UnhandledMessage(
        message: Any,
        sender: ActorRef,
        recipient: ActorRef
        ) =>
      log.error(s"""UnhandledMessage:
                   |  message: $message
                   |  sender: $sender
                   |  recipient: $recipient""".stripMargin)
    case DeadLetter(message: Any, sender: ActorRef, recipient: ActorRef) =>
      log.warning(s"""DeadLetter:
                     |  message: $message
                     |  sender: $sender
                     |  recipient: $recipient""".stripMargin)
  }
}
