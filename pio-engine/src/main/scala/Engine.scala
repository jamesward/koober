package detrevid.predictionio.loadforecasting

import io.prediction.controller.{Engine, EngineFactory}

class Query(
  val circuitId: Int,
  val timestamp: Long
) extends Serializable

class PredictedResult(
  val label: Double
) extends Serializable

class ActualResult(
  val label: Double
) extends Serializable

object ForecastingEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("alg" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
