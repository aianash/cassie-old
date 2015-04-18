package cassie.catalogue

import scala.concurrent._, duration._
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import goshoplane.commons.core.protocols._, Implicits._
import goshoplane.commons.catalogue.CatalogueItem

import scalaz._


class CatalogueSupervisor extends Actor with ActorLogging {

  private val settings = CatalogueSettings(context.system)

  import protocols._
  import settings._
  import store.CatalogueDatastore
  import context.dispatcher

  private val catalogueDatastore = new CatalogueDatastore(settings)
  catalogueDatastore.init()


  def receive = {

    case GetStoreCatalogue(storeId) =>
      catalogueDatastore.getStoreCatalogue(storeId)
        .map(items => items.map(CatalogueItem.encode(_)).flatten) pipeTo sender()


    case GetStoreCatalogueForTypes(storeId, itemTypes) =>
      catalogueDatastore.getStoreCatalogueForType(storeId, itemTypes)
        .map(items => items.map(CatalogueItem.encode(_)).flatten) pipeTo sender()


    case GetCatalogueItems(itemIds) =>
      val grouped = itemIds.groupBy(_.storeId)

      val futureSeq =
        grouped.toSeq.map {
          case (storeId, itemIds) =>
            catalogueDatastore.getCatalogueItems(storeId, itemIds)
              .map(items => items.map(CatalogueItem.encode(_)).flatten)
        }

      Future.sequence(futureSeq).map(_.flatten) pipeTo sender()

  }

}

object CatalogueSupervisor {
  def props = Props[CatalogueSupervisor]
}