package cassie.core.serialization

import scala.util.Try

import akka.serialization._

import commons.core.util.UnsafeUtil
import commons.catalogue.collection._

import cassie.core.protocols.catalogue._


class CatalogueProtocolSerializer extends Serializer {

  import UnsafeUtil._

  def includeManifest = true

  def identifier = 7891886

  def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case InsertStoreCatalogueItem(items) =>
      (for(one <- Try(items.toBinary)) yield {
        one
      }) get
    case _ => throw new IllegalArgumentException("Provided parameter cannot be serialized by CatalogueProtocolSerializer")
  }

  def fromBinary(binary: Array[Byte], clazz: Option[Class[_]]): AnyRef =
    if(clazz.get isAssignableFrom classOf[InsertStoreCatalogueItem]) {
      InsertStoreCatalogueItem(CatalogueItems(binary))
    } else throw new IllegalArgumentException("Provided parameter bytes array doesnot belong to any of CatalogueProtocolSerializer related objects")

}