package services

import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import scala.concurrent.{ExecutionContext, Future}

trait PredictionIO {
  def predict(eventTime: DateTime): Future[JsValue]
}

@Singleton
class PredictionIOImpl @Inject() (configuration: Configuration, wsClient: WSClient)(implicit executionContext: ExecutionContext) extends PredictionIO {

  val predictionIOUrl = configuration.getString("predictionio.url").get

  override def predict(eventTime: DateTime): Future[JsValue] = {

    wsClient.url(predictionIOUrl).get().map { response =>
      // TODO: use actual PIO data
      // response.json

      Json.obj(
        "demand" -> 10
      )
    }
  }

}
