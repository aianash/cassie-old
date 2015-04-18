package cassie.catalogue.store

import cassie.catalogue.CatalogueSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee

import com.datastax.driver.core.Session

class CatalogueCassandraManager(settings: CatalogueSettings) extends DefaultCassandraManager {
  override def cassandraHost: String = settings.CassandraHost
  override val livePort: Int = settings.CassandraPort
}


trait CatalogueConnector extends SimpleCassandraConnector {
  def settings: CatalogueSettings

  override val manager = new CatalogueCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

  override implicit lazy val session: Session = {
    manager.initIfNotInited(keySpace)
    manager.session
  }
}
