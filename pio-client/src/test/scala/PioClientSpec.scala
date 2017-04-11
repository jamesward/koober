import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.joda.time.DateTime
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}

class PioClientSpec extends AsyncFlatSpec with MustMatchers {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  it should "send data to PIO" in {
    if (sys.env.get("PIO_ACCESS_KEY").isDefined) {
      val source = Source.single(
        Json.obj(
          "status" -> "foo",
          "datetime" -> DateTime.now(),
          "lngLat" -> Json.obj()
        )
      )

      val sink = Sink.fold[Seq[JsValue], JsValue](Seq.empty[JsValue])(_ :+ _)

      val pioFlow = PioFlow(sys.env("PIO_ACCESS_KEY"))

      source.via(pioFlow).runWith(sink).map { jsValues =>
        jsValues.size must equal(1)
      }
    }
    else {
      cancel("You need to set the PIO_ACCESS_KEY env var")
    }
  }

}
