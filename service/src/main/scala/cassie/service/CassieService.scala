package cassie.service

import scala.collection.IndexedSeq

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import commons.microservice._

import cassie.service.components._

object CassieService {

  def main(args: Array[String]) {
    val config = ConfigFactory.load("cassie")
    val system = ActorSystem(config.getString("cassie.actorSystem"), config)
    Microservice(system).start(IndexedSeq(CatalogueComponent))
  }

}