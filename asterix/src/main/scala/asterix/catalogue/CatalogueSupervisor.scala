package asterix.catalogue

import akka.actor.{Actor, ActorRef, ActorLogging, Props, Terminated}
import akka.pattern.ask
import akka.util.Timeout

import java.util.UUID

import scala.concurrent._, duration._
import scala.collection.mutable.{Queue => MutableQueue, Map => MutableMap}
import scala.util.Sorting

import goshoplane.commons.catalogue._
import goshoplane.commons.core.protocols.Implicits._

import cassie.catalogue._, store.CatalogueDatastore

import scala.util.Random

import scalaz._, Scalaz._


case class InjectionJobId(id: Long)
case class InjectionJob(jobId: InjectionJobId, catalogueItems: List[CatalogueItem])

class CatalogueSupervisor(consumer: ActorRef) extends Actor with ActorLogging {

  val settings = AsterixSettings(context.system)

  import CatalogueSupervisor._
  import protocols._
  import settings._
  import context.dispatcher

  val store               = new CatalogueDatastore(CatalogueSettings(context.system))
  val injectors           = MutableMap.empty[ActorRef, Option[InjectionJob]]
  val requests            = MutableQueue.empty[ActorRef]
  val retryQueue          = MutableQueue.empty[CatalogueItem]
  val prefetched          = MutableQueue.empty[CatalogueItem]
  var jobSchedulingStatus = Future {true}


  (0 until NrOfInjectors).foreach { _ =>
    context.actorOf(CatalogueItemInjector.props(store, self))
  }


  def receive = {

    case RegisterInjector =>
      val injector = sender()
      context watch injector
      injectors += (injector -> None)


    case GetInjectionJob =>
      val injector = sender()
      requests += injector
      if(jobSchedulingStatus.isCompleted) jobSchedulingStatus = scheduleNextJob


    case CheckAndScheduleJob =>
      if(jobSchedulingStatus.isCompleted && !requests.isEmpty) jobSchedulingStatus = scheduleNextJob


    case InjectionDone => injectors += (sender() -> None)


    case ErrorWhileInjection(item) => retryQueue += item


    case Terminated(injector) =>
      injectors.get(injector).flatten.map(_.catalogueItems).foreach(_.foreach(retryQueue += _))
      context unwatch injector
  }


  /**
   * Schedules new indexing job
   */
  private def scheduleNextJob: Future[Boolean] =
    getCatalogueBatch flatMap { batch =>
      if(batch.isEmpty && !requests.isEmpty) {
        backOffScheduling
        Future.successful(true)
      } else if(batch.isEmpty && requests.isEmpty) {
        Future.successful(true)
      } else if(!batch.isEmpty && !requests.isEmpty) {
        val sendTo = requests.dequeue
        val job    = InjectionJob(InjectionJobId(Random.nextLong), batch)
        injectors += (sendTo -> job.some)
        sendTo ! ProcessJob(job)
        scheduleNextJob
      } else {
        batch.foreach(prefetched += _)
        Future.successful(true)
      }
    }


  private var currentBackoffIter = 0;

  private def backOffScheduling = {
    if(currentBackoffIter > IndexSchedulingBackoffLimit) currentBackoffIter = 0
    currentBackoffIter += 1
    val interval = (Math.pow(2, currentBackoffIter) - 1 ) / 2
    context.system.scheduler.scheduleOnce(interval milliseconds, self, CheckAndScheduleJob)
  }


  /**
   * Returns catalogue batch
   */
  private def getCatalogueBatch = {

    if(!prefetched.isEmpty) {
      val batch =
        (0 to Math.min(InjectionBatchSize, prefetched.length))
          .foldLeft (List.empty[CatalogueItem]) { (batch, _) =>
            prefetched.dequeue() :: batch
          }
      Future.successful(batch)
    } else if(!retryQueue.isEmpty) {
      val batch =
        (0 to Math.min(InjectionBatchSize, retryQueue.length))
          .foldLeft (List.empty[CatalogueItem]) { (batch, _) =>
            retryQueue.dequeue() :: batch
          }
      Future.successful(batch)
    } else {
      implicit val timeout = Timeout(5 seconds)
      (consumer ?= GetNextBatch(InjectionBatchSize)).map(_.batch)
    }
  }

}



object CatalogueSupervisor {

  case object CheckAndScheduleJob

  def props(consumer: ActorRef) = Props(classOf[CatalogueSupervisor], consumer)
}