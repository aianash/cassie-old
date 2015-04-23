package cassie.catalogue.store

import scala.collection.mutable.{Map => MutableMap}

import java.nio.ByteBuffer

import cassie.catalogue.CatalogueSettings

import com.goshoplane.common._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery

import com.datastax.driver.core.querybuilder.QueryBuilder

import play.api.libs.json._

import goshoplane.commons.catalogue._


class CatalogueItemsByItemType(val settings: CatalogueSettings)
  extends CassandraTable[CatalogueItemsByItemType, CatalogueItem] with CatalogueConnector {

  override def tableName = "catalogue_items"

  object stuid extends LongColumn(this) with PartitionKey[Long]
  object itemType extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Ascending
  object cuid extends LongColumn(this)

  object itemTypeGroups extends SetColumn[CatalogueItemsByItemType, CatalogueItem, String](this)
  object namedType extends StringColumn(this)
  object productTitle extends StringColumn(this)

  // image
  object small extends StringColumn(this)
  object medium extends StringColumn(this)
  object large extends StringColumn(this)

  object attributes extends MapColumn[CatalogueItemsByItemType, CatalogueItem, String, String](this)

  object details extends MapColumn[CatalogueItemsByItemType, CatalogueItem, String, String](this)



  /**
   * [IMP] right we have assumed clothing item is the only
   * catalogue item type present... but individual items
   * should be added very very soon
   */
  override def fromRow(row: Row) = {

    val groups      = itemTypeGroups(row).flatMap(gs => ItemTypeGroup.values.find(_.toString == gs)).toArray

    ClothingItem.create(
      itemId         = CatalogueItemId(storeId = StoreId(stuid(row)), cuid = cuid(row)),
      itemType       = ItemType.valueOf(itemType(row)).getOrElse(ItemType.Unknown),
      itemTypeGroups = ItemTypeGroups(groups),
      namedType      = NamedType(namedType(row)),
      productTitle   = ProductTitle(productTitle(row)),
      productImage   = ProductImage(small(row), medium(row), large(row)),
      attributes     = attributes(row)
    )
  }


  def insertCatalogueItem(catalogueItem: CatalogueItem) = {

    val item = catalogueItem.asInstanceOf[ClothingItem]  // [IMP] Better way coming soon

    insert
      .value(_.stuid,           item.itemId.storeId.stuid)
      .value(_.cuid,            item.itemId.cuid)

      .value(_.itemType,        item.itemType.name)
      .value(_.itemTypeGroups,  item.itemTypeGroups.groups.map(_.toString).toSet)
      .value(_.namedType,       item.namedType.name)

      .value(_.productTitle,    item.productTitle.title)

      .value(_.small,           item.productImage.small)
      .value(_.medium,          item.productImage.medium)
      .value(_.large,           item.productImage.large)

      .value(_.attributes,      item.attributesMap)
  }


  def getCatalogueItemsBy(storeId: StoreId, itemTypes: Seq[ItemType]) =
    select.where(_.stuid eqs storeId.stuid).and(_.itemType in itemTypes.map(_.name).toList)

}