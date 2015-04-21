package asterix.catalogue

import kafka.consumer._

import java.util.Properties

import akka.actor.ActorSystem
import akka.actor.Props

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.{Config, ConfigFactory}


object AsterixServer {

  import protocols._

  def main(args: Array[String]) {

    val config    = ConfigFactory.load("asterix")
    val system    = ActorSystem(config.getString("asterix.catalogue.actorSystem"), config)
    val settings  = AsterixSettings(system)
    val connector = getConnector(settings)
    var consumer  = system.actorOf(Props(classOf[CatalogueItemConsumer], connector))

    // val supervisor = system.actorOf(Props(classOf[IndexingSupervisor], consumer, indexDir))
  }

  /**
   * Returns an instance of ConsumerConnector with settings as an
   * input parameter
   */
  def getConnector(settings: AsterixSettings): ConsumerConnector = {
    val props = new Properties()
    props.put("group.id", settings.GroupId)
    props.put("zookeeper.connect", settings.ZookeeperConnect)
    props.put("auto.offset.reset", settings.AutoOffsetReset)
    props.put("consumer.timeout.ms", settings.ConsumerTimoutMs)
    val config = new ConsumerConfig(props)
    val connector = Consumer.create(config)
    connector
  }
}