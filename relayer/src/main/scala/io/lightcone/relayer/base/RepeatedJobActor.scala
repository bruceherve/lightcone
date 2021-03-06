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

package io.lightcone.relayer.base

import akka.actor._
import scala.concurrent.Future
import scala.concurrent.duration._
import io.lightcone.relayer.data.Notify

// Owner: Hongyu
final case class Job(
    name: String,
    dalayInSeconds: Int,
    run: () => Future[Any],
    initialDalayInSeconds: Int = 0,
    delayBetweenStartAndFinish: Boolean = true,
    private[base] var sequence: Long = 0)

trait RepeatedJobActor { actor: Actor with ActorLogging =>
  import context.dispatcher

  val repeatedJobs: Seq[Job]
  private var jobMap = Map.empty[String, Job]

  override def preStart(): Unit = {
    jobMap = repeatedJobs.map(j => (j.name, j)).toMap
    assert(jobMap.size == repeatedJobs.size, "job name not unique")

    repeatedJobs.foreach { job =>
      context.system.scheduler.scheduleOnce(
        job.initialDalayInSeconds.seconds,
        self,
        Notify("run-job", job.name)
      )
    }
  }

  def receiveRepeatdJobs: Receive = {
    case Notify("run-job", name) =>
      jobMap.get(name) foreach { job =>
        job.sequence += 1
        log.debug(s"running repeated job ${job.name}#${job.sequence}")
        val now = System.currentTimeMillis
        job
          .run()
          .recover {
            case e: Exception =>
              log.error(
                s"occurs error in running repeated job: ${e.getMessage}, cause: ${e.getCause}, trace: ${e.printStackTrace()}"
              )
          }
          .map { _ =>
            val timeTook =
              if (job.delayBetweenStartAndFinish) 0
              else (System.currentTimeMillis - now) / 1000

            val delay = Math.max(job.dalayInSeconds - timeTook, 0)
            context.system.scheduler
              .scheduleOnce(delay.seconds, self, Notify("run-job", name))
          }
      }
  }
}
