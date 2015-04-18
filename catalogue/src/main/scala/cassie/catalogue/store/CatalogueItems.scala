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


class CatalogueItems(val settings: CatalogueSettings)
  extends CassandraTable[CatalogueItems, CatalogueItem] with CatalogueConnector {

  override def tableName = "catalogue_items"

  object stuid extends LongColumn(this) with PartitionKey[Long]
  object cuid extends LongColumn(this) with PrimaryKey[Long]

  object itemType extends StringColumn(this)
  object itemTypeGroups extends SetColumn[CatalogueItems, CatalogueItem, String](this)
  object namedType extends StringColumn(this)
  object productTitle extends StringColumn(this)

  object attributes extends MapColumn[CatalogueItems, CatalogueItem, String, String](this)

  object details extends MapColumn[CatalogueItems, CatalogueItem, String, String](this)



  /**
   * [IMP] right we have assumed clothing item is the only
   * catalogue item type present... but individual items
   * should be added very very soon
   */
  override def fromRow(row: Row) = {

    val attr        = attributes(row)

    val colors      = attr.get("colors").map(c => genericArrayOps(c.split(",")).toSeq).getOrElse(Seq.empty[String])
    val sizes       = attr.get("sizes") .map(s => genericArrayOps(s.split(",")).toSeq).getOrElse(Seq.empty[String])
    val brand       = attr.get("brand") .getOrElse("unknown")
    val description = attr.get("description").getOrElse("")
    val price       = attr.get("price").map(augmentString(_).toFloat).getOrElse(-1.0f)

    val groups      = itemTypeGroups(row).flatMap(gs => ItemTypeGroup.values.find(_.toString == gs)).toArray

    ClothingItem(
      itemId         = CatalogueItemId(storeId = StoreId(stuid(row)), cuid = cuid(row)),
      itemType       = ItemType.valueOf(itemType(row)).getOrElse(ItemType.Unknown),
      itemTypeGroups = ItemTypeGroups(groups),
      namedType      = NamedType(namedType(row)),
      productTitle   = ProductTitle(productTitle(row)),
      colors         = Colors(colors),
      sizes          = Sizes(sizes),
      brand          = Brand(brand),
      description    = Description(description),
      price          = Price(price)
    )
  }


  def insertCatalogueItem(catalogueItem: CatalogueItem) = {

    val item = catalogueItem.asInstanceOf[ClothingItem]  // [IMP] Better way coming soon

    val attr = MutableMap[String, String]()
    attr += ("colors"      -> item.colors.values.mkString(","))
    attr += ("sizes"       -> item.sizes.values.mkString(","))
    attr += ("brand"       -> item.brand.name)
    attr += ("description" -> item.description.text)
    attr += ("price"       -> item.price.value.toString)

    insert
      .value(_.stuid,           item.itemId.storeId.stuid)
      .value(_.cuid,            item.itemId.cuid)
      .value(_.itemType,        item.itemType.name)
      .value(_.itemTypeGroups,  item.itemTypeGroups.groups.map(_.toString).toSet)
      .value(_.namedType,       item.namedType.name)
      .value(_.productTitle,    item.productTitle.title)
      .value(_.attributes,      attr.toMap)
  }


  def getCatalogueItemBy(itemId: CatalogueItemId) =
    select.where(_.stuid eqs itemId.storeId.stuid).and(_.cuid eqs itemId.cuid)


  def getCatalogueItemsBy(storeId: StoreId) =
    select.where(_.stuid eqs storeId.stuid)


  def getCatalogueItemsBy(storeId: StoreId, itemIds: Seq[CatalogueItemId]) =
    select.where(_.stuid eqs storeId.stuid).and(_.cuid in itemIds.map(_.cuid).toList)
}