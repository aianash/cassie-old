package asterix.catalogue.protocols

import com.goshoplane.common._

import asterix.catalogue._

sealed trait CatalogueInjectorMessages

case object RegisterInjector extends CatalogueInjectorMessages
case object GetInjectionJob extends CatalogueInjectorMessages
case object InjectionDone extends CatalogueInjectorMessages
case class ErrorWhileInjection(iten: SerializedCatalogueItem) extends CatalogueInjectorMessages

case class ProcessJob(job: InjectionJob) extends CatalogueInjectorMessages