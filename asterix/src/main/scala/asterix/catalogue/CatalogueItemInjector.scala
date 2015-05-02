package asterix.catalogue

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.ListBuffer

import akka.actor.{Actor, Props, ActorRef, ActorLogging}

import goshoplane.commons.catalogue._

import cassie.catalogue.store.CatalogueDatastore


class CatalogueItemInjector(store: CatalogueDatastore, supervisor: ActorRef) extends Actor with ActorLogging {

  import CatalogueItemInjector._
  import protocols._
  import context.dispatcher

  override def preStart() {
    supervisor ! RegisterInjector
    context.system.scheduler.scheduleOnce(500 milliseconds, supervisor, GetInjectionJob)
  }

  def receive = idle

  def working: Receive = {
    case InjectionComplete(jobId) =>
      log.info(s"Injection for job id ${jobId.id} completed")
      supervisor ! InjectionDone(jobId)
      log.info("Requested next injection job")
      supervisor ! GetInjectionJob
      context become idle
  }


  def idle: Receive = {
    case ProcessJob(InjectionJob(jobId, items)) =>
      context become working
      log.info(s"Starting injection job ${jobId.id} with ${items.size} items")
      store.insertCatalogueItems(items) foreach(_ => self ! InjectionComplete(jobId))
  }

}


object CatalogueItemInjector {
  case class InjectionComplete(jobId: InjectionJobId)

  def props(store: CatalogueDatastore, supervisor: ActorRef) =
    Props(classOf[CatalogueItemInjector], store, supervisor)

}
