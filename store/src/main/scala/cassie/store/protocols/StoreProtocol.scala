package cassie.store.protocols

import com.goshoplane.common._

import goshoplane.commons.core.protocols.Replyable

sealed trait StoreMessages

case class IsExistingStore(email: String)
  extends StoreMessages with Replyable[Option[StoreId]]

case class CreateStore(storeType: StoreType, info: StoreInfo)
  extends StoreMessages with Replyable[StoreId]

case class UpdateStore(storeId: StoreId, info: StoreInfo)
  extends StoreMessages with Replyable[Boolean]

case class GetStore(storeId: StoreId, fields: Seq[StoreInfoField])
  extends StoreMessages with Replyable[Store]

case class GetStores(storeIds: Seq[StoreId], fields: Seq[StoreInfoField])
  extends StoreMessages with Replyable[Seq[Store]]