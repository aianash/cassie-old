package cassie.service

import scaldi.akka.AkkaInjectable._

import com.typesafe.config.ConfigFactory

import com.twitter.finagle.Thrift
import com.twitter.util.Await

import akka.actor.ActorSystem

import cassie.service.injectors._
import cassie.core.injectors._

/**
 * Cassie Server that starts the CassieService
 *
 * {{{
 *   service/target/start cassie.service.CassieServer
 * }}}
 */
object CassieServer {

  def main(args: Array[String]) {

    val config = ConfigFactory.load("cassie")

    implicit val appModule = new CassieServiceModule :: new AkkaModule(config)

    implicit val system = inject [ActorSystem]

    val serviceId    = CassieSettings(system).ServiceId
    val datacenterId = CassieSettings(system).DatacenterId

    val server = CassieService.start

    scala.sys.addShutdownHook {
      val waitF = server.close()
      Await.ready(waitF)
    }
  }

}