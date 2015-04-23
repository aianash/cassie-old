package cassie.store.store

import scala.collection.mutable.{Map => MutableMap}

import java.nio.ByteBuffer

import cassie.store.StoreSettings

import scalaz.{Store => _, _}, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.goshoplane.common._

import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery

import com.datastax.driver.core.querybuilder.QueryBuilder

import goshoplane.commons.core.factories._

class Stores extends CassandraTable[Stores, Store] {

  override def tableName = "stores"

  object stuid extends LongColumn(this) with PartitionKey[Long]
  object storeType extends StringColumn(this)

  object fullname extends OptionalStringColumn(this)
  object handle extends OptionalStringColumn(this)

  object itemTypes extends SetColumn[Stores, Store, String](this)

  // address
  object lat extends OptionalDoubleColumn(this)
  object lng extends OptionalDoubleColumn(this)
  object addressTitle extends OptionalStringColumn(this)
  object addressShort extends OptionalStringColumn(this)
  object addressFull extends OptionalStringColumn(this)
  object pincode extends OptionalStringColumn(this)
  object country extends OptionalStringColumn(this)
  object city extends OptionalStringColumn(this)

  // avatar
  object small extends OptionalStringColumn(this)
  object medium extends OptionalStringColumn(this)
  object large extends OptionalStringColumn(this)

  object email extends OptionalStringColumn(this)
  object phoneNums extends SetColumn[Stores, Store, String](this)


  override def fromRow(row: Row) = {
    val gpsLoc   = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    var addressO =
      Common.address( gpsLoc,
                      addressTitle(row),
                      addressShort(row),
                      addressFull(row),
                      pincode(row),
                      country(row),
                      city(row))

    var storeNameO = Common.storeName(fullname(row), handle(row))
    val avatarO    = Common.storeAvatar(small(row), medium(row), large(row))
    val phoneO     = Common.phoneContact(phoneNums(row).toSeq)
    val itemTypesO = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some


    Store(
      storeId   = StoreId(stuid = stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = Common.storeInfo(storeNameO, itemTypesO, addressO, avatarO, email(row), phoneO).getOrElse(StoreInfo())
    )
  }


  def insertStore(store: Store) =
    insert
      .value(_.stuid,         store.storeId.stuid)
      .value(_.storeType,     store.storeType.name)
      .value(_.fullname,      store.info.name.flatMap(_.full))
      .value(_.handle,        store.info.name.flatMap(_.handle))
      .value(_.itemTypes,     store.info.itemTypes.map(_.map(_.name).toSet).getOrElse(Set.empty[String]))
      .value(_.lat,           store.info.address.flatMap(_.gpsLoc.map(_.lat)))
      .value(_.lng,           store.info.address.flatMap(_.gpsLoc.map(_.lng)))
      .value(_.addressTitle,  store.info.address.flatMap(_.title))
      .value(_.addressShort,  store.info.address.flatMap(_.short))
      .value(_.addressFull,   store.info.address.flatMap(_.full))
      .value(_.pincode,       store.info.address.flatMap(_.pincode))
      .value(_.country,       store.info.address.flatMap(_.country))
      .value(_.city,          store.info.address.flatMap(_.city))
      .value(_.small,         store.info.avatar.flatMap(_.small))
      .value(_.medium,        store.info.avatar.flatMap(_.medium))
      .value(_.large,         store.info.avatar.flatMap(_.large))
      .value(_.email,         store.info.email)
      .value(_.phoneNums,     store.info.phone.map(_.numbers.toSet).getOrElse(Set.empty[String]))


  def getStoreBy(storeId: StoreId, fields: Seq[StoreInfoField]) = {
    val selectors = fieldToSelectors(fields)

    val select =
      new SelectQuery(this, QueryBuilder.select(selectors: _*).from(tableName), fromRow)

    select.where(_.stuid eqs storeId.stuid)
  }


  private def fieldToSelectors(fields: Seq[StoreInfoField]) = {
    fields.flatMap {
      case StoreInfoField.Name            => Seq("fullname", "handle")
      case StoreInfoField.ItemTypes       => Seq("itemTypes")
      case StoreInfoField.Address         => Seq("lat", "lng", "addressTitle", "addressShort", "pincode", "country", "city")
      case StoreInfoField.Avatar          => Seq("small", "medium", "large")
      case StoreInfoField.Contacts        => Seq("phoneNums", "email")
      case _                              => Seq.empty[String]
    } ++ Seq("stuid", "storeType")
  }

}