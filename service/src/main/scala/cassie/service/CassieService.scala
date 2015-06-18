package cassie.service

import scala.util.{Failure => TFailure, Success => TSuccess, Try}
import scala.util.control.NonFatal
import scala.concurrent._, duration._

import java.net.InetSocketAddress

import scalaz._, Scalaz._
import scalaz.std.option._
import scalaz.syntax.monad._

import scaldi.Injector
import scaldi.akka.AkkaInjectable._

import akka.actor.{Actor, Props}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.event.Logging

import com.goshoplane.common._
import com.goshoplane.cassie.service._

import goshoplane.commons.core.protocols.Implicits._

import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.builder.ServerBuilder

import org.apache.thrift.protocol.TBinaryProtocol

import cassie._
import cassie.catalogue._, catalogue.protocols._
import cassie.store._, cassie.store.protocols._

class CassieService(implicit inj: Injector) extends Cassie[TwitterFuture] {

  import CassieService._

  implicit val system = inject [ActorSystem]

  val log = Logging.getLogger(system, this)

  import system._ // importing execution context from implicit dispatcher

  val settings = CassieSettings(system)

  implicit val defaultTimeout = Timeout(1 seconds)

  private val Catalogue = system.actorOf(CatalogueSupervisor.props)
  private val Store     = system.actorOf(StoreSupervisor.props)


  ////////////////////
  // Catalogue APIs //
  ////////////////////

  def getStoreCatalogue(storeId: StoreId) = {
    val itemsF = Catalogue ?= GetStoreCatalogue(storeId)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting store's catalogue items for" +
                        s" store id ${storeId.stuid}"

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  def getStoreCatalogueForType(storeId: StoreId, itemTypes: Seq[ItemType]) = {
    val itemsF = Catalogue ?= GetStoreCatalogueForTypes(storeId, itemTypes)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting store's catalogue items for store id ${storeId.stuid} and type " +
                        s" and itemTypes = " + itemTypes.mkString(", ")

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  def getCatalogueItems(itemIds: Seq[CatalogueItemId], detailType: CatalogeItemDetailType) = {
    val itemsF = Catalogue ?= GetCatalogueItems(itemIds)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting catalogue items for catalogue items id = " +
                        itemIds.map(id => s"{id.storeId.stuid}.{id.cuid}").mkString(", ")

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  ////////////////
  // Store APIs //
  ////////////////

  def createOrUpdateStore(storeType: StoreType, info: StoreInfo) = {
    val storeIdF: Future[StoreId] =
      info.email.map { email =>
        (Store ?= IsExistingStore(email)).flatMap { storeIdO =>
          storeIdO match {
            case Some(storeId) => (Store ?= UpdateStore(storeId, info)).map(_ => storeId)
            case None          =>  Store ?= CreateStore(storeType, info)
          }
        }
      } getOrElse {
        Store ?= CreateStore(storeType, info)
      }

    awaitResult(storeIdF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while creating or updating store " +
                        s" for storeType = ${storeType}"

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  def getStore(storeId: StoreId, fields: Seq[StoreInfoField]) = {
    val storeF = Store ?= GetStore(storeId, fields)

    awaitResult(storeF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while getting store detail for " +
                        s" storeId = ${storeId.stuid}" +
                        s" and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  def getStores(storeIds: Seq[StoreId], fields: Seq[StoreInfoField]) = {
    val storesF = Store ?= GetStores(storeIds, fields)

    awaitResult(storesF, 500 milliseconds, {
      case NonFatal(ex) =>
        val statement = s"Error while get stores' infos for " +
                        s"storeIds = " + storeIds.map(_.stuid).mkString(", ") +
                        s" and fields = " + fields.mkString(", ")

        log.error(ex, statement)
        TFailure(CassieException(statement))
    })
  }


  /**
   * A helper method to await on Scala Future and encapsulate the result into TwitterFuture
   */
  private def awaitResult[T, U >: T](future: Future[T], timeout: Duration, ex: PartialFunction[Throwable, Try[U]]): TwitterFuture[U] = {
    TwitterFuture.value(Try {
      Await.result(future, timeout)
    } recoverWith(ex) get)
  }

}


object CassieService {

  def start(implicit inj: Injector) = {
    val settings = CassieSettings(inject [ActorSystem])

    val protocol = new TBinaryProtocol.Factory()
    val service  = new Cassie$FinagleService(inject [CassieService], protocol)
    val address  = new InetSocketAddress(settings.CassiePort)

    ServerBuilder()
      .codec(ThriftServerFramedCodec())
      .name(settings.ServiceName)
      .bindTo(address)
      .build(service)
  }
}