package cassie.store

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.actor.{Extension, ExtensionId, ExtensionIdProvider}

import com.typesafe.config.{Config, ConfigFactory}


class StoreSettings(cfg: Config) extends Extension {

  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "cassie.store")
    config
  }

  val CassandraHost     = config.getString("cassie.store.cassandraHost")
  val CassandraPort     = config.getInt("cassie.store.cassandraPort")
  val CassandraKeyspace = config.getString("cassie.store.cassandraKeyspace")
  val ServiceId         = config.getLong("cassie.service.id")
  val DatacenterId      = config.getLong("cassie.datacenter.id")
}


object StoreSettings extends ExtensionId[StoreSettings]
  with ExtensionIdProvider {

  override def lookup = StoreSettings

  override def createExtension(system: ExtendedActorSystem) =
    new StoreSettings(system.settings.config)

  override def get(system: ActorSystem) = super.get(system)
}