package cassie.catalogue

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class CatalogueSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "cassie.catalogue")
    config
  }

  val CassandraHost     = config.getString("cassie.catalogue.cassandraHost")
  val CassandraPort     = config.getInt("cassie.catalogue.cassandraPort")
  val CassandraKeyspace = config.getString("cassie.catalogue.cassandraKeyspace")
  val ServiceId         = config.getLong("cassie.service.id")
  val DatacenterId      = config.getLong("cassie.datacenter.id")
}


object CatalogueSettings extends ExtensionId[CatalogueSettings]
  with ExtensionIdProvider {

  override def lookup = CatalogueSettings

  override def createExtension(system: ExtendedActorSystem) =
    new CatalogueSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}