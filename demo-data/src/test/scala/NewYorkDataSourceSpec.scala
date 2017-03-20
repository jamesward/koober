import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.libs.json.JsObject

class NewYorkDataSourceSpec extends AsyncFlatSpec with Matchers {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  it should "get some records" in {
    val numMonths = 1
    val sampleRate = 100

    val source = NewYorkDataSource(numMonths, sampleRate)

    val sink = Sink.seq[JsObject]

    val recordsFuture = source.runWith(sink)

    recordsFuture.map { records =>
      assert(records.nonEmpty)
      // todo: more asserts
    }
  }

}
