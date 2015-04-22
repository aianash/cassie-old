package asterix.catalogue

import goshoplane.commons.catalogue._

case class InjectionJobId(id: Long)
case class InjectionJob(jobId: InjectionJobId, catalogueItems: Seq[CatalogueItem])