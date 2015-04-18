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

class StoresByEmail extends CassandraTable[StoresByEmail, Store] {

  override def tableName = "stores_by_email"

  object email extends StringColumn(this) with PartitionKey[String]
  object stuid extends LongColumn(this)
  object storeType extends StringColumn(this)

  override def fromRow(row: Row) =
    Store(
      storeId   = StoreId(stuid(row)),
      storeType = StoreType.valueOf(storeType(row)).getOrElse(StoreType.Unknown),
      info      = StoreInfo())


  def insertStore(store: Store) =
    store.info.email.map { email =>
      insert
        .value(_.email,     email)
        .value(_.stuid,     store.storeId.stuid)
        .value(_.storeType, store.storeType.name)
    }


  def getStoreBy(email: String) = select.where(_.email eqs email)
}