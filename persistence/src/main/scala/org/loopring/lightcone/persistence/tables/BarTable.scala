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

package org.loopring.lightcone.persistence.tables

import org.loopring.lightcone.persistence.base._
import scala.reflect.ClassTag
import slick.jdbc.MySQLProfile.api._
import org.loopring.lightcone.proto.core._
import org.loopring.lightcone.proto.persistence._

private[persistence] class BarTable(tag: Tag)
  extends BaseTable[Bar](tag, "T_BARS") {

  def id = hash

  def hash = columnHash("hash", O.PrimaryKey)
  def a = column[String]("A")
  def b = columnAddress("B")
  def c = columnAmount("C")
  def d = column[Long]("D")

  def * = (
    hash,
    a,
    b,
    c,
    d
  ) <> ((Bar.apply _).tupled, Bar.unapply)
}