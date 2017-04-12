import java.time.{Duration, ZonedDateTime}

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

import scala.collection.immutable.Iterable
import scala.util.Random

object FakeDataSource {

  val baseLat: Double = 40.7550506592
  val baseLng: Double = -73.96534729

  def randomJson(dateTime: ZonedDateTime): JsObject = {
    val randomLat = baseLat + (Random.nextDouble() - 0.5)
    val randomLng = baseLng + (Random.nextDouble() - 0.5)

    Json.obj(
        "lngLat" -> Json.obj(
          "lat" -> randomLat,
          "lng" -> randomLng
        ),
        "status" -> "pickup",
        "datetime" -> new DateTime(dateTime.toEpochSecond * 1000)
    )
  }

  def apply(numRecords: Int, startDate: ZonedDateTime, endDate: ZonedDateTime, numClusters: Int, demandDistPerCluster: Int): Source[JsObject, NotUsed] = {

    val timeBetween = Duration.between(startDate, endDate)

    val numBaseRecords = (numRecords * 0.20).toInt

    // 20% of numRecords get random dates
    val baseRandomIterable = Iterable.fill(numBaseRecords) {
      val randomSecondsToAddToStart: Long = (timeBetween.getSeconds * Random.nextDouble()).toLong
      val randomZonedDateTime = startDate.plusSeconds(randomSecondsToAddToStart)
      randomJson(randomZonedDateTime)
    }

    val baseRandomSource = Source[JsObject](baseRandomIterable)

    // 80% of numRecords get clustered with the same date and location
    // todo: is this correctly partitioning?
    val numClusteredRecords = numRecords - numBaseRecords
    val numRecordsPerCluster = (numClusteredRecords.toFloat / numClusters).ceil.toInt
    val clustersIterator = Iterator.fill(numClusteredRecords)(Unit).grouped(numRecordsPerCluster).flatMap { partition =>
      val randomSecondsToAddToStart: Long = (timeBetween.getSeconds * Random.nextDouble()).toLong
      val randomZonedDateTime = startDate.plusSeconds(randomSecondsToAddToStart)
      val json = randomJson(randomZonedDateTime)
      partition.map(_ => json)
    }

    val clusteredSource = Source.fromIterator(() => clustersIterator)

    baseRandomSource ++ clusteredSource
  }

}
