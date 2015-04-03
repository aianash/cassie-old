include 'common.thrift'

namespace java com.goshoplane.cassie.service

service Cassie {
   list<common.SerializedCatalogueItem> getStoreCatalogue(1:common.StoreId storeId)
   list<common.SerializedCatalogueItem> getStoreCatalogueForType(1:common.StoreId storeId, 2:list<common.ItemType> itemTypes)
   list<common.SerializedCatalogueItem> getSummarizedCatalogue(1:list<common.CatalogueItemId> catalogueItemIds)
   list<common.SerializedCatalogueItem> getDetailedCatalogue(1:list<common.CatalogueItemId> catalogueItemIds)
}