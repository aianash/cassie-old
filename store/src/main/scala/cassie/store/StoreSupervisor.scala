package cassie.store

import scala.concurrent._, duration._
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import com.goshoplane.common._

import goshoplane.commons.core.services._
import goshoplane.commons.core.protocols._, Implicits._
import goshoplane.commons.catalogue.CatalogueItem

import scalaz.{Store => _, _}

class StoreSupervisor extends Actor with ActorLogging {

  private val settings = StoreSettings(context.system)

  import protocols._
  import settings._
  import store.StoreDatastore
  import context.dispatcher

  private val UUID = context.actorOf(UUIDGenerator.props(ServiceId, DatacenterId))
  context watch UUID

  private val storeDatastore = new StoreDatastore(settings)
  storeDatastore.init()


  def receive = {

    case IsExistingStore(email) =>
      storeDatastore.isExistingStore(email) pipeTo sender()


    // [TO DO] Check and validate store information
    case CreateStore(storeType, info) =>
      implicit val timeout = Timeout(1 seconds)

      val storeIdF =
        for {
          stuid <- UUID ?= NextId("store")
          _     <- storeDatastore.insertStore(Store(StoreId(stuid), storeType, info))
        } yield StoreId(stuid)

      storeIdF pipeTo sender()



    case UpdateStore(storeId, info) =>
      // [TO DO]



    case GetStores(storeIds, fields) =>
      Future.sequence(storeIds.map(storeDatastore.getStore(_, fields))) pipeTo sender()


    case GetStore(storeId, fields) =>
      storeDatastore.getStore(storeId, fields) pipeTo sender()

  }
}


object StoreSupervisor {
  def props = Props[StoreSupervisor]
}