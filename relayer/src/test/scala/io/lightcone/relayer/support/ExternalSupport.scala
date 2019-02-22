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
import org.rnorth.ducttape.TimeoutException
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.ContainerLaunchException
import scala.concurrent.Await
import io.lightcone.relayer.actors._
import akka.pattern._
import io.lightcone.relayer.data.cmc.GetExternalTokenTickers

trait ExternalSupport extends DatabaseModuleSupport {
  me: CommonSpec =>

  actors.add(CMCCrawlerActor.name, CMCCrawlerActor.start)

  try Unreliables.retryUntilTrue(
    10,
    TimeUnit.SECONDS,
    () => {
      val f = (actors.get(CMCCrawlerActor.name) ? GetExternalTokenTickers.Req())
        .mapTo[GetExternalTokenTickers.Res]
      val res = Await.result(f, timeout.duration)
      res.tickers.nonEmpty && res.tickers.nonEmpty
    }
  )
  catch {
    case e: TimeoutException =>
      throw new ContainerLaunchException(
        "Timed out waiting for CMCCrawlerActor init.)"
      )
  }
}
