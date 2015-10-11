package cassie.catalogue

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.util.Timeout

import commons.catalogue._, items._, attributes._
import commons.owner.{StoreId, BrandId}

import goshoplane.commons.core.protocols._, Implicits._

import protocols._

object TestCassie {

  def main(args: Array[String]) {
    val system = ActorSystem("test")
    val catalogue = system.actorOf(CatalogueService.props)

    val brandId = BrandId(12345L)
    val itemId = CatalogueItemId(123459L)
    val variantId = VariantId(1234567L)

    val title = ProductTitle("mens polo neck tshirt")
    val namedType = NamedType("polo neck tshirt")
    val brand = Brand("Brand name")
    val price = Price(1234.0F)
    val sizes = ClothingSizes(Seq(ClothingSize.S))
    val colors = Colors(Seq("RED"))
    val itemStyles = ClothingStyles(Seq(ClothingStyle.TeesTop))
    val description = Description("hello")
    val stylingTips = StylingTips("asdfasdf")
    val gender = Male
    val images = Images("http://goshoplane.com", Seq("http://goshoplane.com"))
    val itemUrl = ItemUrl("http://goshoplane.com")

    val branditem =
      MensTShirt.builder.forBrand
                .ids(brandId, itemId, variantId)
                .title(title)
                .namedType(namedType)
                .clothing(brand, price, sizes, colors, itemStyles, description, stylingTips, gender, images, itemUrl)
                .build

    val storeId = StoreId(987654L)
    val storeitem =
      MensTShirt.builder.forStore.using(branditem)
                .ids(storeId, itemId, variantId)
                .build

    implicit val timeout = Timeout(2 seconds)

    // catalogue ! InsertStoreCatalogueItem(List(storeitem))

    val itemsF = catalogue ?= GetStoreCatalogueItems(List((itemId, storeId)))
    itemsF foreach { i =>
      i.items foreach { item =>
        println(item.productTitle)
      }
    }
    Await.result(itemsF, 2 seconds)
  }

}