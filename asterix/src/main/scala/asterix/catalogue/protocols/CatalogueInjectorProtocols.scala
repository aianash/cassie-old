package asterix.catalogue.protocols

import goshoplane.commons.catalogue.CatalogueItem

import asterix.catalogue._

sealed trait CatalogueInjectorMessages

case object RegisterInjector extends CatalogueInjectorMessages
case object GetInjectionJob extends CatalogueInjectorMessages
case object InjectionDone extends CatalogueInjectorMessages
case class ErrorWhileInjection(iten: CatalogueItem) extends CatalogueInjectorMessages

case class ProcessJob(job: InjectionJob) extends CatalogueInjectorMessages