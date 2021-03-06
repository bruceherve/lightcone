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

package io.lightcone.relayer.support

import java.util.concurrent.TimeUnit
import com.typesafe.config._
import io.lightcone.relayer._
import io.lightcone.relayer.actors._
import io.lightcone.relayer.data._
import io.lightcone.relayer.validator._
import org.rnorth.ducttape.TimeoutException
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.ContainerLaunchException
import akka.pattern._
import io.lightcone.persistence.CursorPaging
import scala.concurrent.Await

trait ActivitySupport extends DatabaseModuleSupport {
  me: CommonSpec =>

  override def afterAll: Unit = {
    dcm.close()
    super.afterAll
  }

  val config1 = ConfigFactory
    .parseString(activityConfigStr)
    .withFallback(ConfigFactory.load())

  val dcm = new DatabaseConfigManager(config1)

  actors.add(
    ActivityActor.name,
    ActivityActor
      .start(
        system,
        config1,
        ec,
        timeProvider,
        timeout,
        actors,
        dbModule,
        dcm,
        true
      )
  )

  try Unreliables.retryUntilTrue(
    10,
    TimeUnit.SECONDS,
    () => {
      val f =
        (actors.get(ActivityActor.name) ? GetActivities.Req(
          paging = Some(CursorPaging(size = 10))
        )).mapTo[GetActivities.Res]
      val res = Await.result(f, timeout.duration)
      res.activities.isEmpty || res.activities.nonEmpty
    }
  )
  catch {
    case e: TimeoutException =>
      throw new ContainerLaunchException(
        "Timed out waiting for MetadataManagerActor init.)"
      )
  }

  actors.add(
    ActivityValidator.name,
    MessageValidationActor(
      new ActivityValidator(),
      ActivityActor.name,
      ActivityValidator.name
    )
  )
}
