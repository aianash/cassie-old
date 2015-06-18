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
  extends CassandraTable[CatalogueItemsByItemType, SerializedCatalogueItem] with CatalogueConnector {

  override def tableName = "catalogue_items_by_types"

  object stuid extends LongColumn(this) with PartitionKey[Long]
  object itemType extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Ascending
  object cuid extends LongColumn(this) with PrimaryKey[Long] with ClusteringOrder[Long] with Ascending

  // serializer identifiers
  object sid extends StringColumn(this)
  object stype extends StringColumn(this)
  object stream extends BlobColumn(this)


  /**
   * [IMP] right we have assumed clothing item is the only
   * catalogue item type present... but individual items
   * should be added very very soon
   */
  override def fromRow(row: Row) = {
    val storeId        = StoreId(stuid = stuid(row))
    val serializerType = SerializerType.valueOf(stype(row)).getOrElse(SerializerType.Msgpck)

    SerializedCatalogueItem(
      itemId       = CatalogueItemId(storeId = storeId, cuid = cuid(row)),
      serializerId = SerializerId(sid = sid(row), stype = serializerType),
      stream       = stream(row)
    )
  }


  def insertCatalogueItem(itemType: ItemType, item: SerializedCatalogueItem) =
    insert
      .value(_.stuid,     item.itemId.storeId.stuid)
      .value(_.itemType,  itemType.name)
      .value(_.cuid,      item.itemId.cuid)

      .value(_.sid,       item.serializerId.sid)
      .value(_.stype,     item.serializerId.stype.name)
      .value(_.stream,    item.stream)



  def getCatalogueItemsBy(storeId: StoreId, itemTypes: Seq[ItemType]) =
    select.where(_.stuid eqs storeId.stuid).and(_.itemType in itemTypes.map(_.name).toList)

}