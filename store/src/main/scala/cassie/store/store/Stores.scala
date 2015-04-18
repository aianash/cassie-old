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
    // creating address
    val gpsLoc = for(lat <- lat(row); lng <- lng(row)) yield GPSLocation(lat, lng)
    var addressO = gpsLoc.map(v => PostalAddress(gpsLoc = v.some))
    addressO = addressTitle(row).map {t => addressO.getOrElse(PostalAddress()).copy(title   =  t.some) }
    addressO = addressShort(row).map {s => addressO.getOrElse(PostalAddress()).copy(short   =  s.some) }
    addressO = addressFull(row) .map {f => addressO.getOrElse(PostalAddress()).copy(full    =  f.some) }
    addressO = pincode(row)     .map {p => addressO.getOrElse(PostalAddress()).copy(pincode =  p.some) }
    addressO = country(row)     .map {c => addressO.getOrElse(PostalAddress()).copy(country =  c.some) }
    addressO = city(row)        .map {c => addressO.getOrElse(PostalAddress()).copy(city    =  c.some) }

    var storeNameO = StoreName().some
    storeNameO = fullname(row).flatMap(f => storeNameO.map(_.copy(full = f.some))) orElse storeNameO
    storeNameO = handle(row)  .flatMap(h => storeNameO.map(_.copy(handle = h.some))) orElse storeNameO

    var nameO  = fullname(row).flatMap(f => StoreName(full = f.some).some)
    nameO = handle(row).flatMap(h => nameO.map(_.copy(handle = h.some))) orElse nameO


    val itemTypesO = itemTypes(row).flatMap(ItemType.valueOf(_)).toSeq.some
    val avatarO = StoreAvatar(small = small(row), medium = medium(row), large = large(row)).some
    val phoneO  = PhoneContact(phoneNums(row).toSeq).some

    Store(
      storeId   = StoreId(stuid = stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = StoreInfo(
        name      = nameO,
        itemTypes = itemTypesO,
        address   = addressO,
        avatar    = avatarO,
        email     = email(row),
        phone     = phoneO
      )
    )
  }


  def insertStore(store: Store) =
    insert
      .value(_.stuid, store.storeId.stuid)
      .value(_.storeType, store.storeType.name)
      .value(_.fullname, store.info.name.flatMap(_.full))


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
      case _                                  => Seq.empty[String]
    } ++ Seq("stuid", "storeType")
  }

}