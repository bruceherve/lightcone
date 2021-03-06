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

package io.lightcone.relayer.ethereum

import akka.actor.ActorRef
import io.lightcone.relayer.base._
import org.slf4s.Logging

trait EventDispatcher {
  def dispatch(evt: Any): Unit
}

class EventDispatcherImpl(actors: Lookup[ActorRef])
    extends EventDispatcher
    with Logging {
  var targets = Map.empty[Class[_], Set[String]]
  var broadcastTargets = Map.empty[Class[_], Set[DeployedAsShardedFixedSize[_]]]

  def register(
      cls: Class[_],
      actorNames: String*
    ) = {
    val t = targets.getOrElse(cls, Set.empty[String]) ++ actorNames.toSet
    targets = targets + (cls -> t)
    this
  }

  def registerBroadcast(
      cls: Class[_],
      ts: DeployedAsShardedFixedSize[_]*
    ) = {
    val t = broadcastTargets.getOrElse(
      cls,
      Set.empty[DeployedAsShardedFixedSize[_]]
    ) ++ ts.toSet
    broadcastTargets = broadcastTargets + (cls -> t)
    this
  }

  def dispatch(evt: Any) = {
    targets.get(evt.getClass) match {
      case None =>
        log.error(
          s"unable to dispatch message of type: ${evt.getClass.getName}"
        )

      case Some(names) =>
        val (found, notFound) = names.partition(actors.contains)
        if (notFound.nonEmpty) {
          log.error(
            s"unable to dispatch message to actor with the following names: $notFound"
          )
        }

        found.map(actors.get).foreach(_ ! evt)
    }

    broadcastTargets.get(evt.getClass) match {
      case None =>
        log.error(
          s"unable to broadcast message of type: ${evt.getClass.getName}"
        )
      case Some(receivers) =>
        log.debug(s"EventDispatcher dispatch ${evt}")
        receivers.foreach(_.broadcast(evt))
    }
  }

}
