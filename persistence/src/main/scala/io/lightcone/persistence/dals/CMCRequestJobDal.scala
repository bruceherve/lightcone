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

package io.lightcone.persistence.dals

import io.lightcone.core._
import io.lightcone.persistence._
import io.lightcone.persistence.base._
import scala.concurrent._

trait CMCRequestJobDal extends BaseDalImpl[CMCRequestJobTable, CMCRequestJob] {

  def saveJob(job: CMCRequestJob): Future[CMCRequestJob]

  def updateStatusCode(
      jobId: Int,
      code: Int,
      time: Long
    ): Future[ErrorCode]
  def updateSuccessfullyPersisted(jobId: Int): Future[ErrorCode]
  def findLatest(): Future[Option[CMCRequestJob]]
}
