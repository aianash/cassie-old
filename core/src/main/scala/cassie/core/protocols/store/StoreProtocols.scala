package cassie.core.protocols.store

import goshoplane.commons.core.protocols._, Implicits._

import commons.owner._

sealed trait StoreProtocol

case class InsertStore(store: Store) extends StoreProtocol with Replyable[Boolean]
case class GetStore(storeId: StoreId) extends StoreProtocol with Replyable[Store]