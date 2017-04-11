import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, MustMatchers}
import play.api.libs.json.JsObject

class NewYorkDataSourceSpec extends AsyncFlatSpec with MustMatchers with BeforeAndAfterAll {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  it should "get some records" in {
    val numMonths = 1
    val sampleRate = 100

    val source = NewYorkDataSource(numMonths, sampleRate)

    val sink = Sink.seq[JsObject]

    val recordsFuture = source.runWith(sink)

    recordsFuture.map { records =>
      records must not be 'empty
    }
  }

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

}
