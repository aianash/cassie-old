package cassie.service.components

import akka.actor.ActorSystem

import commons.microservice.Component

import cassie.catalogue._

case object CatalogueComponent extends Component {
  val name = "catalogue-service"
  val runOnRole = "catalogue-service"
  def start(system: ActorSystem) = {
    system.actorOf(CatalogueService.props, name)
  }
}