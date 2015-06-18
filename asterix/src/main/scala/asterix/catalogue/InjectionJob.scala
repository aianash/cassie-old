package asterix.catalogue

import com.goshoplane.common._

case class InjectionJobId(id: Long)
case class InjectionJob(jobId: InjectionJobId, catalogueItems: Seq[SerializedCatalogueItem])