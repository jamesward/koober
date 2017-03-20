package services

import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}


trait PredictionIO {
  def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsValue]
}

@Singleton
class PredictionIOImpl @Inject() (configuration: Configuration, wsClient: WSClient)(implicit executionContext: ExecutionContext) extends PredictionIO {

  val predictionIOUrl = configuration.getString("predictionio.url").get

  override def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsValue] = {

    wsClient.url(predictionIOUrl).get().map { response =>
      response.json
    }
  }
}
