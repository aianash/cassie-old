package asterix.catalogue

import scala.concurrent._, duration._
import scala.collection.mutable.{Queue => MutableQueue, Map => MutableMap}
import scala.util.{Sorting, Random}
import scala.util.control.NonFatal

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorLogging, Props, Terminated}
import akka.pattern.ask
import akka.util.Timeout

import com.goshoplane.common._

import goshoplane.commons.core.protocols.Implicits._

import cassie.catalogue._, store.CatalogueDatastore

import scalaz._, Scalaz._


class CatalogueSupervisor(consumer: ActorRef) extends Actor with ActorLogging {

  val settings = AsterixSettings(context.system)

  import CatalogueSupervisor._
  import protocols._
  import settings._
  import context.dispatcher

  val store               = new CatalogueDatastore(CatalogueSettings(context.system))
  val injectors           = MutableMap.empty[ActorRef, Option[InjectionJob]]
  val requests            = MutableQueue.empty[ActorRef]
  val retryQueue          = MutableQueue.empty[SerializedCatalogueItem]
  val prefetched          = MutableQueue.empty[SerializedCatalogueItem]
  var jobSchedulingStatus = Future.successful(true)

  store.init()

  (0 until NrOfInjectors).foreach { idx =>
    context.actorOf(CatalogueItemInjector.props(store, self), "injector-" + idx)
  }


  def receive = {

    case RegisterInjector =>
      val injector = sender()
      context watch injector
      injectors += (injector -> None)


    case GetInjectionJob =>
      val injector = sender()
      requests += injector
      log.info(s"Injector at ${injector.path.toStringWithoutAddress} requested an injection job")
      if(jobSchedulingStatus.isCompleted) {
        log.info("Scheduling next job on request")
        jobSchedulingStatus = scheduleNextJob
        jobSchedulingStatus onSuccess { case _ =>
          log.info("Successfully completed a schedule cycle")
        }
      }


    case CheckAndScheduleJob =>
      if(jobSchedulingStatus.isCompleted && !requests.isEmpty) {
        log.info("Scheduling next job after check")
        jobSchedulingStatus = scheduleNextJob
        jobSchedulingStatus onSuccess { case _ =>
          log.info("Successfully completed a schedule cycle")
        }
      }

    case InjectionDone(jobId) => injectors += (sender() -> None)


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
        log.info("Emtpy batch and no requests therefore nothing to schedule")
        Future.successful(true)
      } else if(!batch.isEmpty && !requests.isEmpty) {
        val sendTo = requests.dequeue
        val job    = InjectionJob(InjectionJobId(Random.nextLong), batch)
        injectors += (sendTo -> job.some)
        sendTo ! ProcessJob(job)
        log.info(s"Scheduled an injection job of ${batch.size} to injector at ${sendTo.path.toStringWithoutAddress}")
        scheduleNextJob
      } else {
        log.info("No requests therefore adding batch to prefetch")
        batch.foreach(prefetched += _)
        Future.successful(true)
      }
    }


  private var currentBackoffIter = 0;

  private def backOffScheduling = {
    if(currentBackoffIter > InjectionBackoffLimit) currentBackoffIter = 0
    currentBackoffIter += 1
    val interval = (Math.pow(2, currentBackoffIter) - 1 ) / 2 * InjectionBackoffTime
    log.info(s"Backing off before next job scheduling by $interval milliseconds")
    context.system.scheduler.scheduleOnce(interval milliseconds, self, CheckAndScheduleJob)
  }


  /**
   * Returns catalogue batch
   */
  private def getCatalogueBatch =
    if(!prefetched.isEmpty) {
      val batch =
        (0 to Math.min(InjectionBatchSize, prefetched.length))
          .foldLeft (List.empty[SerializedCatalogueItem]) { (batch, _) =>
            prefetched.dequeue() :: batch
          }

      log.info(s"Created a batch of ${batch.size} from prefetched items")

      Future.successful(batch)
    } else if(!retryQueue.isEmpty) {
      val batch =
        (0 to Math.min(InjectionBatchSize, retryQueue.length))
          .foldLeft (List.empty[SerializedCatalogueItem]) { (batch, _) =>
            retryQueue.dequeue() :: batch
          }

      log.info(s"Created a batch of ${batch.size} from retry queue")

      Future.successful(batch)
    } else {
      implicit val timeout = Timeout(ConsumerTimoutMs seconds)
      val batchF = (consumer ?= GetNextBatch(InjectionBatchSize)).map(_.batch)
      batchF onFailure { case NonFatal(ex) => log.error(ex, "Error while getting batch from kafka") }
      batchF foreach { batch => log.info(s"Got batch of ${batch.size} items from kafka") }
      batchF
    }

}



object CatalogueSupervisor {

  case object CheckAndScheduleJob

  def props(consumer: ActorRef) = Props(classOf[CatalogueSupervisor], consumer)
}