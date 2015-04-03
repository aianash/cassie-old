include 'common.thrift'

namespace java com.goshoplane.cassie.service

service Cassie {
   list<common.SerializedCatalogueItem> getStoreCatalogues(1:common.StoreId storeId)
   list<common.SerializedCatalogueItem> getStoreCataloguesOfType(1:common.StoreId storeId, 2:common.ItemType itemType)
   list<common.SerializedCatalogueItem> getCatalogue(1:list<common.CatalogueItemId> catalogueItemIds)
}