package cassie.service

import scala.concurrent.duration._

import akka.actor.{ActorSystem, Extension, ExtensionId, ExtensionIdProvider, ExtendedActorSystem}

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Cassie specific settings
 */
class CassieSettings(cfg: Config) extends Extension {

  // validate cassie config
  final val config: Config = {
    val config = cfg.withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "cassie")
    config
  }

  val ActorSystem  = config.getString("cassie.actorSystem")
  val ServiceId    = config.getLong("cassie.service.id")
  val ServiceName  = config.getString("cassie.service.name")
  val DatacenterId = config.getLong("cassie.datacenter.id")

  val CassieHost   = config.getString("cassie.host")
  val CassiePort   = config.getInt("cassie.port")
}


object CassieSettings extends ExtensionId[CassieSettings] with ExtensionIdProvider {
  override def lookup = CassieSettings

  override def createExtension(system: ExtendedActorSystem) =
    new CassieSettings(system.settings.config)

  override def get(system: ActorSystem): CassieSettings = super.get(system)
}