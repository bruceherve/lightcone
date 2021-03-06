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

package io.lightcone.relayer

import io.lightcone.core.TokenTicker
import io.lightcone.persistence.TokenTickerRecord

package object implicits {

  implicit def tickerRecordConvertor(ticker: TokenTickerRecord): TokenTicker =
    TokenTicker(
      ticker.tokenAddress,
      ticker.price,
      ticker.volume24H,
      ticker.percentChange1H,
      ticker.percentChange24H,
      ticker.percentChange7D
    )

  implicit def tickersRecordConvertor(
      tickers: Seq[TokenTickerRecord]
    ): Seq[TokenTicker] =
    tickers.map(tickerRecordConvertor)
}
