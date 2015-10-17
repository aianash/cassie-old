package cassie.store

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import akka.actor.{PoisonPill, Terminated}
import akka.util.Timeout
import akka.pattern.pipe

import goshoplane.commons.core.protocols._, Implicits._

import cassie.core.protocols.store._


class StoreService extends Actor with ActorLogging {

  private val settings = StoreSettings(context.system)

  import settings._
  import store.StoreDatastore
  import context.dispatcher

  private val catalogueDatastore = new StoreDatastore(settings)
  catalogueDatastore.init()

  def receive = {

    case InsertStore(store) => catalogueDatastore.insertStore(store) pipeTo sender()

    case GetStore(storeId)  => catalogueDatastore.getStore(storeId) pipeTo sender()

  }

}