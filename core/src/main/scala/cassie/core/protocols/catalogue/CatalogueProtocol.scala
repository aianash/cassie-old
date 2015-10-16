package cassie.core.protocols.catalogue

import goshoplane.commons.core.protocols.Replyable

import commons.catalogue._, collection._
import commons.owner.StoreId

sealed trait CatalogueMessages

case class InsertStoreCatalogueItem(items: CatalogueItems) extends CatalogueMessages with Replyable[Boolean]
case class InsertBrandCatalogueItem(items: CatalogueItems) extends CatalogueMessages with Replyable[Boolean]
case class GetBrandCatalogueItems(ids: Seq[CatalogueItemId]) extends CatalogueMessages with Replyable[CatalogueItems]
case class GetStoreCatalogueItems(keys: Seq[(CatalogueItemId, StoreId)]) extends CatalogueMessages with Replyable[CatalogueItems]