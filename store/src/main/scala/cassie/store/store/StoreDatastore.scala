package cassie.store.store

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

import scalaz.{Store => _, _}, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import cassie.store.StoreSettings

import com.goshoplane.common._

import goshoplane.commons.catalogue._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context


sealed class StoreDatastore(val settings: StoreSettings)
  extends StoreConnector {

  object Stores extends Stores
  object StoresByEmail extends StoresByEmail

  def init()(implicit executor: ExecutionContext) {
    val creation =
      for {
        _ <- Stores.create.future()
        _ <- StoresByEmail.create.future()
      } yield true

    Await.ready(creation, 2 seconds)
  }



  def isExistingStore(email: String)(implicit executor: ExecutionContext) =
    StoresByEmail.getStoreBy(email).one().map(_.map(_.storeId))



  def insertStore(store: Store) = {
    val batch = BatchStatement()
    batch add Stores.insertStore(store)

    StoresByEmail.insertStore(store) foreach {batch add _}

    batch.future()
  }



  def getStore(storeId: StoreId, fields: Seq[StoreInfoField])(implicit executor: ExecutionContext) =
    Stores.getStoreBy(storeId, fields).one()

}