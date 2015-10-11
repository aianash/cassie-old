package cassie.store.store

import cassie.store.StoreSettings

import com.websudos.phantom.connectors.{SimpleConnector, ContactPoint, KeySpace}


trait StoreConnector extends SimpleConnector {

  def settings: StoreSettings

  implicit val keySpace = KeySpace(settings.CassandraKeyspace)

  val connector = ContactPoint(settings.CassandraHost, settings.CassandraPort)

}