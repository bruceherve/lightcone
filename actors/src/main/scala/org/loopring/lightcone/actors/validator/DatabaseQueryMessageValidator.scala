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

package org.loopring.lightcone.actors.validator

import com.typesafe.config.Config
import org.loopring.lightcone.core.base.ErrorException
import org.loopring.lightcone.proto._

object DatabaseQueryMessageValidator {
  val name = "database_query_validator"
}

final class DatabaseQueryMessageValidator()(
    implicit
    val config: Config
) extends MessageValidator {

  def validate = {
    case req: XSaveOrderReq ⇒
      if (req.order.isEmpty || (req.order.get.state.nonEmpty && req.order.get.state.get.status !=
        XOrderStatus.STATUS_NEW)) throw ErrorException(XError(XErrorCode.ERR_PERSISTENCE_INVALID_DATA))
    case req: XUserCancelOrderReq ⇒
      if (req.orderHashes.isEmpty) throw ErrorException(XError(XErrorCode.ERR_PERSISTENCE_INVALID_DATA))
  }
}
