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

package org.loopring.lightcone.ethereum.event

import com.google.inject.Inject
import org.loopring.lightcone.ethereum.abi._
import org.loopring.lightcone.proto.{
  OrdersCancelledEvent => POrdersCancelledEvent,
  _
}
import scala.concurrent._

class OrdersCancelledEventExtractor @Inject()(implicit ec: ExecutionContext)
    extends EventExtractor[POrdersCancelledEvent] {

  def extract(
      tx: Transaction,
      receipt: TransactionReceipt,
      blockTime: String
    ): Future[Seq[POrdersCancelledEvent]] = Future {
    val header = getEventHeader(tx, receipt, blockTime)
    receipt.logs.zipWithIndex.map { item =>
      {
        val (log, index) = item
        loopringProtocolAbi
          .unpackEvent(log.data, log.topics.toArray) match {
          case Some(event: OrdersCancelledEvent.Result) =>
            Some(
              POrdersCancelledEvent(
                header = Some(header.withLogIndex(index)),
                broker = event.address,
                orderHashes = event._orderHashes,
                owner = event.address
              )
            )
          case _ =>
            None
        }
      }
    }.filter(_.nonEmpty).map(_.get)
  }
}
