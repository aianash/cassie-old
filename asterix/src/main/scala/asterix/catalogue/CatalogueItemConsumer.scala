package asterix.catalogue

import scala.util.{Try, Success, Failure}

import akka.actor.Actor

import kafka.consumer._
import kafka.serializer.StringDecoder

import goshoplane.commons.catalogue.CatalogueItem

import goshoplane.commons.catalogue.kafka.serializers.SerializedCatalogueItemDecoder

class CatalogueItemConsumer(connector: ConsumerConnector) extends Actor {

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
        case Failure(ex)    => replyTo ! akka.actor.Status.Failure(ex)
      }
  }

  /**
   * Return next batch of catalogue items from kafka topic
   */
  private def getNextBatch(batchSize: Int) = {
    Try {
      (0 to batchSize).foldLeft (List.empty[CatalogueItem]) { (batch, _) =>

        Try {iterator.next().message}
          .map(CatalogueItem.decode(_))
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

}