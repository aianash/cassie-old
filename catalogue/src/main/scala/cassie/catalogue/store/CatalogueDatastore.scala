package cassie.catalogue.store

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

import java.nio.ByteBuffer

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import com.websudos.phantom.dsl._

import cassie.catalogue.CatalogueSettings

import commons.catalogue._, collection._



sealed class CatalogueDatastore(val settings: CatalogueSettings) extends CatalogueConnector {

  object BrandCatalogueItems extends ConcreteBrandCatalogueItems(settings)
  object StoreCatalogueItems extends ConcreteStoreCatalogueItems(settings)


  def init(): Boolean = {
    val creation =
      for {
        _ <- BrandCatalogueItems.create.ifNotExists.future()
        _ <- StoreCatalogueItems.create.ifNotExists.future()
      } yield true

    Await.result(creation, 2 seconds)
  }

  def insertStoreCatalogueItems(items: Seq[CatalogueItem]): Future[Boolean] = {
    val batch =
      items.foldLeft (Batch.logged) { (b, i) =>
        b.add(BrandCatalogueItems.insetBrandCatalogueItem(i.itemId, ByteBuffer.wrap(CatalogueItem.brandBinary(i))))
         .add(StoreCatalogueItems.insetStoreCatalogueItem(i.itemId, i.ownerId.asInstanceOf[StoreId], CatalogueItem.storeBinary(i).map(ByteBuffer.wrap(_)).get))
      }
    batch.future().map(_ => true)
  }

  def insertBrandCatalogueItems(items: Seq[CatalogueItem]): Future[Boolean] = {
    val batch =
      items.foldLeft (Batch.logged) { (b, i) =>
        b.add(BrandCatalogueItems.insetBrandCatalogueItem(i.itemId, ByteBuffer.wrap(CatalogueItem.brandBinary(i))))
      }
    batch.future().map(_ => true)
  }

  def getBrandCatalogueItems(itemIds: Seq[CatalogueItemId]): Future[CatalogueItems] = {
    val itemsF = BrandCatalogueItems.getBrandCatalogueItemsFor(itemIds).fetch()
    itemsF map { items =>
      val catalogueItems = items map { it2 =>
        val bytes = byteBufferToByteArray(it2._2)
        CatalogueItem(bytes)
      }
      CatalogueItems(catalogueItems)
    }
  }

  def getStoreCatalogueItems(keys: Seq[(CatalogueItemId, StoreId)]): Future[CatalogueItems] =
    (for {
      brandItems <- BrandCatalogueItems.getBrandCatalogueItemsFor(keys.map(_._1)).fetch()
      storeItems <- StoreCatalogueItems.getStoreCatalogueItemsFor(keys).fetch() if brandItems.nonEmpty
    } yield {
      val id2brandItem = brandItems.toMap
      val catalogueItems = storeItems flatMap { storeItem =>
        id2brandItem.get(storeItem._1).map { x =>
          CatalogueItem(byteBufferToByteArray(storeItem._3), CatalogueItem(byteBufferToByteArray(x)))
        }
      }
      CatalogueItems(catalogueItems)
    }) recoverWith {
      case _: NoSuchElementException => Future.successful(CatalogueItems(Seq.empty[CatalogueItem]))
    }

  private def byteBufferToByteArray(buf: ByteBuffer) = {
    val bytes = Array.ofDim[Byte](buf.remaining)
    buf.get(bytes)
    bytes
  }

}
