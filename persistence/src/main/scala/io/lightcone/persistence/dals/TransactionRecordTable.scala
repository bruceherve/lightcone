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

import com.google.protobuf.ByteString
import io.lightcone.ethereum.event._
import io.lightcone.persistence.base._
import io.lightcone.relayer.data._
import slick.jdbc.MySQLProfile.api._

class TransactionRecordTable(shardId: String)(tag: Tag)
    extends BaseTable[TransactionRecord](
      tag,
      s"TRANSACTION_RECORD_${shardId.toUpperCase}"
    ) {

  implicit val txStatusColumnType = enumColumnType(TxStatus)
  implicit val recordTypeColumnType = enumColumnType(
    TransactionRecord.RecordType
  )
  implicit val dataColumnType = eventDataColumnType()

  def id = txHash
  def txHash = columnHash("tx_hash")
  def txStatus = column[TxStatus]("tx_status")
  def blockHash = columnHash("block_hash")
  def blockNumber = column[Long]("block_number")
  def blockTimestamp = column[Long]("block_timestamp")
  def txFrom = columnAddress("tx_from")
  def txTo = columnAddress("tx_to")
  def txValue = column[ByteString]("tx_value")
  def txIndex = column[Int]("tx_index")
  def logIndex = column[Int]("log_index")
  def eventIndex = column[Int]("event_index")
  def gasPrice = column[Long]("gas_price")
  def gasLimit = column[Int]("gas_limit")
  def gasUsed = column[Int]("gas_used")
  def owner = columnAddress("owner")
  def recordType = column[TransactionRecord.RecordType]("record_type")
  def marketId = column[Long]("market_id")
  def marketName = column[String]("market_name")
  def eventData = column[Option[TransactionRecord.EventData]]("event_data")
  def createdAt = column[Long]("created_at")
  def sequenceId = column[Long]("sequence_id", O.PrimaryKey)

  // indexes
  def idx_owner = index("idx_owner", (owner), unique = false)

  def idx_owner_type =
    index("idx_owner_type", (owner, recordType), unique = false)

  def idx_from_to_type =
    index("idx_from_to_type", (txFrom, txTo, recordType), unique = false)

  def headerProjection =
    (
      txHash,
      txStatus,
      blockHash,
      blockNumber,
      blockTimestamp,
      txFrom,
      txTo,
      txValue,
      txIndex,
      logIndex,
      eventIndex,
      gasPrice,
      gasLimit,
      gasUsed
    ) <> ({ tuple =>
      Option((EventHeader.apply _).tupled(tuple))
    }, { paramsOpt: Option[EventHeader] =>
      val params = paramsOpt.getOrElse(EventHeader())
      EventHeader.unapply(params)
    })

  def * =
    (
      headerProjection,
      owner,
      recordType,
      marketId,
      marketName,
      sequenceId,
      eventData
    ) <> ((TransactionRecord.apply _).tupled, TransactionRecord.unapply)
}
