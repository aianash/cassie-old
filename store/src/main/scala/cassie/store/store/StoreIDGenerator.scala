package goshoplane.commons.core.services

import scala.util.{Try, Success, Failure}

import akka.actor.{Actor, Props, ActorLogging}

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap

import goshoplane.commons.core.protocols._, Implicits._

class StoreIDGenerator extends Actor with ActorLogging {
  import StoreIDGenerator._

  private[this] val StoreCityEndBitPosition = 56
  private[this] val NearestStoreLongitudeEndBitPosition = 40
  private[this] val NearestStoreLatitudeEndBitPosition = 25
  private[this] val StoreLongitudeRelativeToNearestStoreEndBitPosition = 14
  private[this] val StoreLatitudeRelativeToNearestStoreEndBitPosition = 4

  private def NormalizeLatitude(latitude: Double) : Double = {
    latitude + 90
  }

  private def NormalizeLongitude(longitude: Double): Double = {
    longitude + 180
  }

  private def GetNearestStoreLatitude(latitude: Double) : Long = {
    (NormalizeLatitude(latitude) * 100).toLong
  }

  private def GetNearestStoreLongitude(longitude: Double) : Long = {
    (NormalizeLongitude(longitude) * 100).toLong
  }
  
  def IsStoreIdExists(storeId: Long): Boolean = {
    // check whether storeId exists in cassandra storeID store
    false
  }

  private def GetNextNearestRelativeLatitude(relativeLatitude: Long) : Long =
  {
    if (relativeLatitude >= 1000 || relativeLatitude < 0)
    {
      throw new IllegalArgumentException("")
    }

    (relativeLatitude/100  * 100) + (((relativeLatitude % 100) + 1) % 100)
  }

  def GenerateStoreId(longitude: Double, latitude: Double, city: Byte, storeType: Byte): Long =
  {
    // City -8 Longitude-16 Latitude-15 RelativeLongitude-11 RelativeLatitude-10 StoreType-4
    val storeCity = city.toLong << StoreCityEndBitPosition
    val nearStoreLongitude = GetNearestStoreLongitude(longitude) << NearestStoreLongitudeEndBitPosition
    val nearStoreLatitude = GetNearestStoreLatitude(latitude) << NearestStoreLatitudeEndBitPosition
    val relativeLongitude = StoreIDGenerator.GetValueAfterDecimalPlaces(3, 5, longitude) << StoreLongitudeRelativeToNearestStoreEndBitPosition;
    var relativeLatitude = StoreIDGenerator.GetValueAfterDecimalPlaces(3, 5, latitude);
    var outputstoreId = 1L
    Int storeCountOnGpsLocation = 1
    Bool storeIdExists = false
    do {
      outputstoreId = storeCity | nearStoreLongitude | nearStoreLatitude | relativeLongitude | (relativeLatitude << StoreLatitudeRelativeToNearestStoreEndBitPosition) | storeType.toLong;
      storeIdExists = IsStoreIdExists(outputstoreId)
      if (!storeIdExists)
      {
        relativeLatitude = GetNextNearestRelativeLatitude(relativeLatitude)
        storeCountOnGpsLocation += 1
      }
    }while(storeIdExists && storeCountOnGpsLocation < 1000)
    //Throw exception if we could not find the store ID due to collosion. Never possible.
    outputstoreId;
  }
}


object StoreIDGenerator {
  val LengthOfPowerArray = 7

  val powerOf10 = Array(1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 1000000L)

  def GetValueAfterDecimalPlaces(startIndex : Int, endIndex: Int, value: Double): Long =
  {
    if (startIndex < 1 || startIndex > endIndex || endIndex - startIndex + 1 > StoreIDGenerator.LengthOfPowerArray)
    {
      throw new IllegalArgumentException("")
    }

    val rightValue =  (value*StoreIDGenerator.powerOf10(endIndex)).toLong;
    val leftValue =  ((value * StoreIDGenerator.powerOf10(startIndex-1)).toLong * StoreIDGenerator.powerOf10(endIndex-startIndex+1));
    (rightValue - leftValue);
  }
}