package cassie.store.store

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}

import com.websudos.phantom.dsl._

import cassie.store.StoreSettings

import commons.owner._

class StoreDatastore(val settings: StoreSettings) extends StoreConnector {

  object Stores extends ConcreteStores(settings)

  /**
   * To initialize cassandra tables
   */
  def init(): Boolean = {
    val creation =
      for {
        _ <- Stores.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  def insertStore(store: Store) = Stores.insertStore(store).future().map(_ => true)

  def getStore(storeId: StoreId) = Stores.getStoreFor(storeId).one()

}