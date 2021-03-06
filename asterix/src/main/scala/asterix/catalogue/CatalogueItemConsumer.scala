package asterix.catalogue

import scala.util.{Try, Success, Failure}

import java.util.Properties

import akka.actor.{Actor, Props, ActorLogging}

import kafka.consumer._
import kafka.serializer.StringDecoder

import goshoplane.commons.catalogue.CatalogueItem
import goshoplane.commons.catalogue.kafka.serializers.SerializedCatalogueItemDecoder

import com.goshoplane.common._

import scalaz._, Scalaz._


class CatalogueItemConsumer(connector: ConsumerConnector) extends Actor with ActorLogging {

  val settings = AsterixSettings(context.system)

  import protocols._
  import settings._

  val filterSpec = new Whitelist(KafkaTopic)
  val streams    = connector.createMessageStreamsByFilter(filterSpec, 1, new StringDecoder(), new SerializedCatalogueItemDecoder())
  val stream     = streams(0)
  val iterator   = stream.iterator()

  def receive = {
    case GetNextBatch(batchSize) =>
      val replyTo = sender();

      getNextBatch(batchSize) match {
        case Success(batch) => replyTo ! CatalogueItemBatch(batch = batch)
        case Failure(ex)    =>
          log.error(ex, "Error while getting next catalogue batch")
          replyTo ! CatalogueItemBatch(batch = List.empty[SerializedCatalogueItem])
      }
  }



  /**
   * Return next batch of catalogue items from kafka topic
   */
  private def getNextBatch(batchSize: Int) =
    Try {
      (0 until batchSize).foldLeft(List.empty[SerializedCatalogueItem]) { (batch, _) =>

        Try({iterator.next().message}).map(_.some)
          .recover {
            case _: ConsumerTimeoutException => None
          } match {
            case Success(None)                => batch
            case Success(Some(catalogueItem)) => catalogueItem :: batch
            case Failure(ex)                  => throw ex
          }
      }
    }


}


object CatalogueItemConsumer {

  def props(connector: ConsumerConnector) = Props(classOf[CatalogueItemConsumer], connector)

  def props(settings: AsterixSettings): Props = props(defaultConnector(settings))


  /**
   * Returns an instance of ConsumerConnector with settings as an
   * input parameter
   */
  def defaultConnector(settings: AsterixSettings): ConsumerConnector = {
    import settings._
    val props = new Properties()

    props.put("group.id",             GroupId)
    props.put("zookeeper.connect",    ZookeeperConnect)
    props.put("auto.offset.reset",    AutoOffsetReset)
    props.put("consumer.timeout.ms",  ConsumerTimoutMs.toString)

    val config = new ConsumerConfig(props)
    Consumer.create(config)
  }

}