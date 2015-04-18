package cassie.store.store

import cassie.store.StoreSettings

import com.websudos.phantom.zookeeper.{SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.Implicits._
import com.websudos.phantom.iteratee.Iteratee

import com.datastax.driver.core.Session

class StoreCassandraManager(settings: StoreSettings) extends DefaultCassandraManager {
  override def cassandraHost: String = settings.CassandraHost
  override val livePort: Int = settings.CassandraPort
}


trait StoreConnector extends SimpleCassandraConnector {
  def settings: StoreSettings

  override val manager = new StoreCassandraManager(settings)

  val keySpace = settings.CassandraKeyspace

  override implicit lazy val session: Session = {
    manager.initIfNotInited(keySpace)
    manager.session
  }
}
