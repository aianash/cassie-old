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

import cassie.catalogue._, protocols._


class CassieService(implicit inj: Injector) extends Cassie[TwitterFuture] {

  import CassieService._

  implicit val system = inject [ActorSystem]

  val log = Logging.getLogger(system, this)

  import system._ // importing execution context from implicit dispatcher

  val settings = CassieSettings(system)

  implicit val defaultTimeout = Timeout(1 seconds)

  private val Catalogue = system.actorOf(CatalogueSupervisor.props)

  ////////////////////
  // Catalogue APIs //
  ////////////////////

  def getStoreCatalogue(storeId: StoreId) = {
    val itemsF = Catalogue ?= GetStoreCatalogue(storeId)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) => TFailure(CassieException("Error while creating user"))
    })
  }



  def getStoreCatalogueForType(storeId: StoreId, itemTypes: Seq[ItemType]) = {
    val itemsF = Catalogue ?= GetStoreCatalogueForTypes(storeId, itemTypes)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) => TFailure(CassieException("Error while creating user"))
    })
  }

  def getCatalogueItems(itemIds: Seq[CatalogueItemId], detailType: CatalogeItemDetailType) = {
    val itemsF = Catalogue ?= GetCatalogueItems(itemIds)

    awaitResult(itemsF, 500 milliseconds, {
      case NonFatal(ex) => TFailure(CassieException("Error while creating user"))
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