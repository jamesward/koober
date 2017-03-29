package services

import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


trait PredictionIO {
  def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsValue]
}

@Singleton
class PredictionIOImpl @Inject() (configuration: Configuration, wsClient: WSClient)(implicit executionContext: ExecutionContext) extends PredictionIO {

  val predictionIOUrl = configuration.getString("predictionio.url").get
  val predictionIOUrls: scala.collection.Map[java.lang.String, java.lang.String] = configuration.getObject("predictionio.urls").get.unwrapped().asScala.mapValues(_.toString)

  override def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsValue] = {
    wsClient.url(predictionIOUrl).get().map { response =>
      response.json
    }
  }

  def predictAll(startDateTime: DateTime, endDateTime: DateTime): Future[JsValue] = {
    val futures: Iterable[Future[WSResponse]] = predictionIOUrls.values.map(wsClient.url(_).get())

    val future: Future[Iterable[WSResponse]] = Future.sequence(futures)

    future.map { wsResponses =>
      wsResponses.foldLeft(Json.obj()) {
        case (accumulator, wsResponse) =>
          accumulator ++ wsResponse.json.as[JsObject]
      }
    }
  }

  def predictForModels(startDateTime: DateTime, endDateTime: DateTime, models: Seq[String]): Future[JsValue] = {
    val futures: Iterable[Future[WSResponse]] = predictionIOUrls.filterKeys(models.contains(_)).values.map(wsClient.url(_).get())

    val future: Future[Iterable[WSResponse]] = Future.sequence(futures)

    future.map { wsResponses =>
      wsResponses.foldLeft(Json.obj()) {
        case (accumulator, wsResponse) =>
          accumulator ++ wsResponse.json.as[JsObject]
      }
    }
  }

  def predictForModel(startDateTime: DateTime, endDateTime: DateTime, model: String): Future[JsValue] = {
    predictionIOUrls.get(model).fold(Future.failed[JsValue](new Exception("Model not found"))) { url =>
      wsClient.url(url).get().map { response =>
        response.json
      }
    }
  }

}
