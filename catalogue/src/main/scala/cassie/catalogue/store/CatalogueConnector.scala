package cassie.catalogue.store

import cassie.catalogue.CatalogueSettings

import com.websudos.phantom.connectors.{SimpleConnector, ContactPoint, KeySpace}


trait CatalogueConnector extends SimpleConnector {

  def settings: CatalogueSettings

  implicit val keySpace = KeySpace(settings.CassandraKeyspace)

  val connector = ContactPoint(settings.CassandraHost, settings.CassandraPort)

}
