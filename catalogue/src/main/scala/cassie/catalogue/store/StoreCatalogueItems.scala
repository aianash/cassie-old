package cassie.catalogue.store

import java.nio.ByteBuffer

import cassie.catalogue.CatalogueSettings

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import commons.catalogue._
import commons.owner.StoreId


sealed class StoreCatalogueItems extends CassandraTable[ConcreteStoreCatalogueItems, (CatalogueItemId, StoreId, ByteBuffer)] {

  override def tableName = "store_catalogue_items"

  // composite key
  // most significant 64 bits: Item Id
  // least siginificant 64 bits: Store Id
  object uuid extends UUIDColumn(this) with PartitionKey[UUID]
  // store item
  object storeItem extends BlobColumn(this)

  def fromRow(row: Row) =
    (CatalogueItemId(uuid(row).getMostSignificantBits), StoreId(uuid(row).getLeastSignificantBits), storeItem(row))

}

abstract class ConcreteStoreCatalogueItems(val settings: CatalogueSettings) extends StoreCatalogueItems {

  def insetStoreCatalogueItem(itemId: CatalogueItemId, storeId: StoreId, item: ByteBuffer)(implicit keySpace: KeySpace) =
    insert.value(_.uuid, new UUID(itemId.cuid, storeId.stuid))
      .value(_.storeItem, item)

  def getStoreCatalogueItemsFor(keys: Seq[(CatalogueItemId, StoreId)])(implicit keyspace: KeySpace) =
    select.where(_.uuid in keys.map(x => new UUID(x._1.cuid, x._2.stuid)).toList)

  def updateStoreCatalogueItemFor(itemId: CatalogueItemId, storeId: StoreId, item: ByteBuffer)(implicit keyspace: KeySpace) =
    update.where(_.uuid eqs new UUID(itemId.cuid, storeId.stuid))
      .modify(_.storeItem setTo item)

}