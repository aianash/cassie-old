package asterix.catalogue

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.Properties

import kafka.consumer._

import akka.actor.ActorSystem
import akka.actor.Props

import com.typesafe.config.{Config, ConfigFactory}


object AsterixServer {

  import protocols._

  def main(args: Array[String]) {

    val config     = ConfigFactory.load("asterix")
    val system     = ActorSystem(config.getString("asterix.catalogue.actorSystem"), config)
    val settings   = AsterixSettings(system)
    var consumer   = system.actorOf(CatalogueItemConsumer.props(settings))
    val supervisor = system.actorOf(CatalogueSupervisor.props(consumer))
  }


}