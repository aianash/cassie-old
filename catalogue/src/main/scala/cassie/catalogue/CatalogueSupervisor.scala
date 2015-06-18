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
import goshoplane.commons.catalogue.CatalogueItem

import com.goshoplane.common._

import scalaz._, Scalaz._


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
      val itemsF =
        catalogueDatastore.getStoreCatalogue(storeId)
        .recover {
          case NonFatal(ex) =>
            log.error(ex, "Caught error {} while getting store catalogue items for store id = {}",
                          ex.getMessage,
                          storeId.stuid)
            Seq.empty[SerializedCatalogueItem] // Intentionall sending empty result if there
                                               // was some database error
        }

      itemsF pipeTo sender()



    case GetStoreCatalogueForTypes(storeId, itemTypes) =>
      val itemsF =
        catalogueDatastore.getStoreCatalogueForType(storeId, itemTypes)
        .recover {
          case NonFatal(ex) =>
            log.error(ex, "Caught error {} while getting store catalogue items for store id = {} and itemTypes = {}",
                          ex.getMessage,
                          storeId.stuid,
                          itemTypes.mkString(", "))
            Seq.empty[SerializedCatalogueItem] // Intentionall sending empty result if there
                                               // was some database error
        }

      itemsF pipeTo sender()



    case GetCatalogueItems(itemIds) =>
      val grouped = itemIds.groupBy(_.storeId)

      val futureSeq =
        grouped.toSeq.map {
          case (storeId, itemIds) =>
            val itemsF = catalogueDatastore.getCatalogueItems(storeId, itemIds)
            itemsF recover {
              // Catching and logging database related errors like connection failure etc
              case NonFatal(ex) =>
                log.error(ex, "Skipping items for storeId = {} because caught error {} while getting catalogue items for item ids = [{}]",
                              storeId.stuid,
                              ex.getMessage,
                              itemIds.map(_.cuid).mkString(", "))
                Seq.empty[SerializedCatalogueItem]
            }
        }

      val itemsF = Future.sequence(futureSeq).map(_.flatten)

      // Logging error if some item ids were missing.
      // Which could be because of no entry in database
      // or some error caught while fetching (handled above)
      itemsF foreach { items =>
        implicit val ordering = Ordering[(Long, Long)].on({ x: CatalogueItemId => (x.storeId.stuid, x.cuid) })
        if(items.size != itemIds.size) {
          val fetchedItemIds = Sorting.stableSort(items.map(_.itemId))
          val givenItemIds   = Sorting.stableSort(itemIds)
          val leftOutIds     = givenItemIds.diff(fetchedItemIds)
          log.error("Some items' details were not fetched, itemIds = {}",
                    leftOutIds.map(id => s"${id.storeId.stuid}.${id.cuid}").mkString(", "))
        }
      }

      // [NOTE] All database related errors are actually caught
      // and handled before sending it to client
      // Error while fetching a particular item are just skipped in
      // the result; so may result in empty sequence of items
      // Intentionally not telling client about the errors
      itemsF pipeTo sender()

  }

}

object CatalogueSupervisor {
  def props = Props[CatalogueSupervisor]
}