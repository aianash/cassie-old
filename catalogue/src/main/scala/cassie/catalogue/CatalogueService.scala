package cassie.catalogue

import scala.concurrent._, duration._
import scala.util.control.NonFatal
import scala.util.Sorting
import scala.math.Ordering

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import goshoplane.commons.core.protocols._, Implicits._

import goshoplane.commons._

import scalaz._, Scalaz._


class CatalogueService extends Actor with ActorLogging {

  private val settings = CatalogueSettings(context.system)

  import protocols._
  import settings._
  import store.CatalogueDatastore
  import context.dispatcher

  private val catalogueDatastore = new CatalogueDatastore(settings)
  catalogueDatastore.init()


  def receive = {

    case InsertStoreCatalogueItem(items) =>
      catalogueDatastore.insertStoreCatalogueItems(items) pipeTo sender()

    case InsertBrandCatalogueItem(items) =>
      catalogueDatastore.insertBrandCatalogueItems(items) pipeTo sender()

    case GetBrandCatalogueItems(itemsIds) =>
      catalogueDatastore.getBrandCatalogueItems(itemsIds) pipeTo sender()

    case GetStoreCatalogueItems(keys) =>
      catalogueDatastore.getStoreCatalogueItems(keys) pipeTo sender()

    case _ =>

  }

}

object CatalogueService {
  def props = Props[CatalogueService]
}