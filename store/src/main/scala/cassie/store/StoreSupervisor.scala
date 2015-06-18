package cassie.store

import scala.concurrent._, duration._
import scala.util.control.NonFatal
import scala.util.Sorting
import scala.math.Ordering
import scala.util.{Try, Failure, Success}

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
      storeDatastore.isExistingStore(email)
      .recoverWith {
        case NonFatal(ex) =>
          log.error(ex, "Caught error = {} while checking existence of store for email = {}",
                        ex.getMessage,
                        email)
          // Do not spill over the exception
          // [To Improve] change exception to custom InternalException
          Future.failed(new Exception("Some internal error occured"))
      } pipeTo sender()



    case CreateStore(storeType, info) =>
      implicit val timeout = Timeout(1 seconds)

      val storeIdF =
        (for {
          stuid <- (UUID ?= NextId("store")).map(_.get)
          _     <- storeDatastore.insertStore(Store(StoreId(stuid), storeType, info))
        } yield StoreId(stuid))
        .recoverWith {
            case NonFatal(ex) =>
              log.error(ex, "Caught error {} while creating store for storeType = {}, datastore in possible error state",
                             ex.getMessage,
                             storeType)

              // Do not spill the exact exception to client of this actor
              // [To Improve] change exception to custom InternalException
              //
              // If this exception has ocuured then certainly datastore
              // is in error state (i.e. tables are in partial state,
              // to handle this case later)
              Future.failed(new Exception("Some internal error occured"))
        } pipeTo sender()



    case UpdateStore(storeId, info) =>
      sender() ! false



    case GetStores(storeIds, fields) =>
      storeDatastore.getStores(storeIds, fields)
      .recover {
        case NonFatal(ex) =>
          log.error(ex, "Caught error {} while getting stores for storeIds = {} and fields = [{}]",
                        storeIds.map(_.stuid).mkString(", "),
                        fields.mkString(", "))

          // Dont spill over the exception, just return no stores
          Seq.empty[Store]
      } andThen {
        case Success(stores) =>
          implicit val ordering = Ordering.by[StoreId, Long](_.stuid)
          if(stores.size != storeIds.size) { // i.e. not all store's info received
            val fetchedStoreIds = Sorting.stableSort(stores.map(_.storeId))
            val givenStoreIds   = Sorting.stableSort(storeIds)
            val leftOutIds      = givenStoreIds.diff(fetchedStoreIds)
            log.error("Store infos' for storeIds = [{}] were not fetched",
                      leftOutIds.map(_.stuid).mkString(", "))
          }
      } pipeTo sender()



    case GetStore(storeId, fields) =>
      storeDatastore.getStore(storeId, fields)
      .recoverWith {
        case NonFatal(ex) =>
          log.error(ex, "Caught error = {} while getting store info for storeId = {}",
                        ex.getMessage,
                        storeId.stuid)

          // Dont spill over the exception
          // [To improve] Custom internal error exception
          Future.failed(new Exception("Some internal error occured"))
      } pipeTo sender()

  }

}


object StoreSupervisor {
  def props = Props[StoreSupervisor]
}