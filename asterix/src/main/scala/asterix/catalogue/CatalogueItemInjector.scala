package asterix.catalogue

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.ListBuffer

import akka.actor.{Actor, Props, ActorRef}

import goshoplane.commons.catalogue._

import cassie.catalogue.store.CatalogueDatastore


class CatalogueItemInjector(store: CatalogueDatastore, supervisor: ActorRef) extends Actor {

  import CatalogueItemInjector._
  import protocols._
  import context.dispatcher

  override def preStart() {
    supervisor ! RegisterInjector
    context.system.scheduler.scheduleOnce(500 milliseconds, supervisor, GetInjectionJob)
  }

  def receive = idle

  def working: Receive = {
    case InjectionComplete =>
      supervisor ! InjectionDone
      supervisor ! GetInjectionJob
      context become idle
  }


  def idle: Receive = {
    case ProcessJob(InjectionJob(jobId, items)) =>
      context become working
      store.insertCatalogueItems(items) foreach(_ => self ! InjectionComplete)
  }

}


object CatalogueItemInjector {
  case object InjectionComplete

  def props(store: CatalogueDatastore, supervisor: ActorRef) =
    Props(classOf[CatalogueItemInjector], store, supervisor)

}
