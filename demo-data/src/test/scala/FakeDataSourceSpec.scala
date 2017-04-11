import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.joda.time.DateTime
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json.JsObject

class FakeDataSourceSpec extends AsyncFlatSpec with MustMatchers {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  it should "generate some records" in {
    val numRecords = 1024
    val numClusters = 10
    val demandDistPerCluster = 10

    val source = FakeDataSource(numRecords, ZonedDateTime.now().minusMonths(1), ZonedDateTime.now(), numClusters, demandDistPerCluster)

    val recordsFuture = source.runFold(Seq.empty[JsObject])(_ :+ _)

    recordsFuture.map { records =>
      records.size must equal (numRecords)
      (records.head \ "datetime").asOpt[DateTime] must be ('defined)
      (records.head \ "datetime").as[DateTime].getYear must equal (ZonedDateTime.now().getYear)
    }
  }

}
