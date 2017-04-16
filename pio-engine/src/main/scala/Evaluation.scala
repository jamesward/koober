package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.AverageMetric
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EngineParams
import org.apache.predictionio.controller.EngineParamsGenerator
import org.apache.predictionio.controller.Evaluation

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import math.{pow, sqrt}

case class RMSEMetric()
  extends AverageMetric[EmptyEvaluationInfo, Query, PredictedResult, ActualResult] {

  override
  def calculate(sc: SparkContext,
                evalDataSet: Seq[(EmptyEvaluationInfo,
                  RDD[(Query, PredictedResult, ActualResult)])]): Double = {
    sqrt(super.calculate(sc, evalDataSet))
  }

  def calculate(query: Query, predicted: PredictedResult, actual: ActualResult): Double =
    pow(predicted.demand - actual.demand, 2)

  override
  def compare(r0: Double, r1: Double): scala.Int = {
    -1 * super.compare(r0, r1)
  }
}

object RMSEEvaluation extends Evaluation {
  engineMetric = (ForecastingEngine(), new RMSEMetric())
}

object EngineParamsList extends EngineParamsGenerator {

  private[this] val baseEP = EngineParams(
    dataSourceParams = DataSourceParams(appName = "koober", evalK = Some(0.8)))

  engineParamsList = Seq(
    baseEP.copy(
      algorithmParamsList = Seq(
        ("algGBTree", GBTreeParams(iterations = 20, maxDepth = 10)),
        ("algGBTree", GBTreeParams(iterations = 20, maxDepth = 20))
      )),
    baseEP.copy(
      algorithmParamsList = Seq(
        ("algRegression", AlgorithmParams(iterations = 1000, miniBatchFraction = 0.5, stepSize = 0.01)),
        ("algRegression", AlgorithmParams(iterations = 5000, miniBatchFraction = 0.5, stepSize = 0.01))
      )),
    baseEP.copy(
      algorithmParamsList = Seq(
        ("multinomialLogistic", LogisticParams(iterations = 4096, numClasses = 200, convergenceTol = 0.01, regParam = 0.5)),
        ("multinomialLogistic", LogisticParams(iterations = 4096, numClasses = 200, convergenceTol = 0.01, regParam = 0.5))
      ))
  )
}

