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

import kamon.Kamon

object KamonSupport {

  def counter(metricName: String) = Kamon.counter(s"c_$metricName")

  def gauge(metricName: String) = Kamon.gauge(s"g_$metricName")

  def histogram(metricName: String) = Kamon.histogram(s"h_$metricName")

  def timer(metricName: String) = Kamon.timer(s"t_$metricName")
}
