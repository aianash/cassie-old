package cassie.catalogue.protocols

import com.goshoplane.common._

import goshoplane.commons.core.protocols.Replyable

import goshoplane.commons.catalogue.CatalogueItem

sealed trait CatalogueMessages

case class GetStoreCatalogue(storeId: StoreId)
  extends CatalogueMessages with Replyable[Seq[SerializedCatalogueItem]]

case class GetStoreCatalogueForTypes(storeId: StoreId, itemTypes: Seq[ItemType])
  extends CatalogueMessages with Replyable[Seq[SerializedCatalogueItem]]

case class GetCatalogueItems(itemIds: Seq[CatalogueItemId])
  extends CatalogueMessages with Replyable[Seq[SerializedCatalogueItem]]

