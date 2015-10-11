package cassie.catalogue.store

import java.nio.ByteBuffer

import cassie.catalogue.CatalogueSettings

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import commons.catalogue._


sealed class BrandCatalogueItems extends CassandraTable[ConcreteBrandCatalogueItems, (CatalogueItemId, ByteBuffer)] {

  override def tableName = "brand_catalogue_items"

  // item id
  object catalogueItemId extends LongColumn(this) with PartitionKey[Long]
  // brand catalogue item
  object brandItem extends BlobColumn(this)

  def fromRow(row: Row) =
    (CatalogueItemId(catalogueItemId(row)), brandItem(row))

}

abstract class ConcreteBrandCatalogueItems(val settings: CatalogueSettings) extends BrandCatalogueItems {

  def insetBrandCatalogueItem(itemId: CatalogueItemId, byte: ByteBuffer)(implicit keySpace: KeySpace) =
    insert.value(_.catalogueItemId, itemId.cuid)
      .value(_.brandItem, byte)

  def getBrandCatalogueItemsFor(itemIds: Seq[CatalogueItemId])(implicit keySpace: KeySpace) =
    select.where(_.catalogueItemId in itemIds.map(_.cuid).toList)

  def updateCatalogueItem(itemId: CatalogueItemId, byte: ByteBuffer)(implicit keySpace: KeySpace) = {
    update.where(_.catalogueItemId eqs itemId.cuid)
      .modify(_.brandItem setTo byte)
  }

}