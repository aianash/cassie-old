package cassie.store.store

import java.nio.ByteBuffer

import cassie.store.StoreSettings

import scalaz.{Store => _, _}, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import commons.owner._

sealed class Stores extends CassandraTable[ConcreteStores, Store] {

  override val tableName = "stores"

  // id
  object storeId extends LongColumn(this) with PartitionKey[Long]

  // data
  object name extends StringColumn(this)
  object website extends OptionalStringColumn(this)

  def fromRow(row: Row) = {
    val sid = StoreId(storeId(row))
    val nameO = StoreName(name(row))
    val websiteO = website(row).map(StoreWebsite(_))

    Store(sid, StoreInfo(nameO, websiteO))
  }

}

abstract class ConcreteStores(settings: StoreSettings) extends Stores {

  def insertStore(store: Store)(implicit keySpace: KeySpace) =
    insert.value(_.storeId, store.storeId.stuid)
      .value(_.name, store.info.name.value)
      .value(_.website, store.info.website.map(_.url))

  def getStoreFor(storeId: StoreId)(implicit keySpace: KeySpace) =
    select.where(_.storeId eqs storeId.stuid)

}