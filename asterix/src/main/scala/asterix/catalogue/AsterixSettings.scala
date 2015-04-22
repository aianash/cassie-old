package asterix.catalogue

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class AsterixSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "asterix.catalogue")
    config
  }

  val GroupId               = config.getString("asterix.catalogue.kafka.group-id")
  val ZookeeperConnect      = config.getString("asterix.catalogue.kafka.zookeeper-connect")
  val AutoOffsetReset       = config.getString("asterix.catalogue.kafka.auto-offset-reset")
  val ConsumerTimoutMs      = config.getInt("asterix.catalogue.kafka.consumer-timeout-ms")
  val KafkaTopic            = config.getString("asterix.catalogue.kafka.topic")
  val ActorSystem           = config.getString("asterix.catalogue.actorSystem")
  val InjectionBatchSize    = config.getInt("asterix.catalogue.injector.batch-size")
  val NrOfInjectors         = config.getInt("asterix.catalogue.injector.nr-of-injectors")
  val InjectionBackoffLimit = config.getInt("asterix.catalogue.injector.backoff-limit")
  val InjectionBackoffTime  = config.getInt("asterix.catalogue.injector.backoff-time")
}


object AsterixSettings extends ExtensionId[AsterixSettings]
  with ExtensionIdProvider {

  override def lookup = AsterixSettings

  override def createExtension(system: ExtendedActorSystem) =
    new AsterixSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}