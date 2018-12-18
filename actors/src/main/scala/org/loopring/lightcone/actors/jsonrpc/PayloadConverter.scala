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

package org.loopring.lightcone.actors.jsonrpc

import org.loopring.lightcone.lib._
import org.json4s.JsonAST.JValue
import scala.reflect.ClassTag
import org.loopring.lightcone.proto.XErrorCode

import scala.reflect.runtime.universe.{typeOf, TypeTag}

class PayloadConverter[T <: Proto[T]: TypeTag, S <: Proto[S]: TypeTag](
    implicit tc: ProtoC[T],
    ts: ProtoC[S],
    cs: ClassTag[S],
    ps: ProtoSerializer) {

  def convertToRequest(str: JValue): T = ps.deserialize[T](str).get

  def convertFromResponse(s: Any): JValue = {
    if (!cs.runtimeClass.isInstance(s))
      throw ErrorException(
        XErrorCode.ERR_INTERNAL_UNKNOWN,
        s"expect ${typeOf[T].typeSymbol.name} get ${s.getClass.getName}"
      )
    ps.serialize[S](s.asInstanceOf[S]).get
  }
}