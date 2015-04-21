package cassie.catalogue.store

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import cassie.catalogue.CatalogueSettings

import com.goshoplane.common._

import goshoplane.commons.catalogue._

import com.websudos.phantom.Implicits.{context => _, _} // donot import execution context



sealed class CatalogueDatastore(val settings: CatalogueSettings)
  extends CatalogueConnector {

  object CatalogueItems extends CatalogueItems(settings)
  object CatalogueItemsByItemType extends CatalogueItemsByItemType(settings)


  def init()(implicit executor: ExecutionContext) {
    val creation =
      for {
        _ <- CatalogueItems.create.future()
        _ <- CatalogueItemsByItemType.create.future()
      } yield true

    Await.ready(creation, 2 seconds)
  }


  def getStoreCatalogue(storeId: StoreId)(implicit executor: ExecutionContext) =
    CatalogueItems.getCatalogueItemsBy(storeId).fetch()


  def getStoreCatalogueForType(storeId: StoreId, itemTypes: Seq[ItemType])(implicit executor: ExecutionContext) =
    CatalogueItemsByItemType.getCatalogueItemsBy(storeId, itemTypes).fetch()


  def getCatalogueItems(storeId: StoreId, itemIds: Seq[CatalogueItemId])(implicit executor: ExecutionContext) =
    CatalogueItems.getCatalogueItemsBy(storeId, itemIds).fetch()


  def getCatalogueItem(itemId: CatalogueItemId)(implicit executor: ExecutionContext) =
    CatalogueItems.getCatalogueItemBy(itemId).one()


  def insertCatalogueItems(items: Seq[CatalogueItem])(implicit executor: ExecutionContext) = {
    val batch = BatchStatement()

    items.foreach { item =>
      batch add CatalogueItems.insertCatalogueItem(item)
      batch add CatalogueItemsByItemType.insertCatalogueItem(item)
    }

    batch.future().map(_ => true)
  }

}
