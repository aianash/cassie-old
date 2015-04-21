package asterix.catalogue.protocols

import goshoplane.commons.catalogue.CatalogueItem
import goshoplane.commons.core.protocols._

sealed trait CatalogueItemConsumerMessage  extends Serializable

case class GetNextBatch(batchSize: Int)
  extends CatalogueItemConsumerMessage with Replyable[CatalogueItemBatch]

case class CatalogueItemBatch(batch: List[CatalogueItem]) extends CatalogueItemConsumerMessage