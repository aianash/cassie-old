package cassie.core

import akka.actor.ActorSystem

import scaldi.Module
import scaldi.Injectable._

import com.typesafe.config.Config

package object injectors {

  class AkkaModule(config: Config) extends Module {
    val name = config.getString("cassie.actorSystem")

    bind [ActorSystem] to ActorSystem(name, config) destroyWith (_.shutdown())
  }

}