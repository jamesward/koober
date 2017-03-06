package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{CustomQuerySerializer, P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{LogisticRegressionModel, LogisticRegressionWithLBFGS}
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.feature.StandardScalerModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.optimization.SquaredL2Updater
import org.apache.spark.mllib.regression.{LinearRegressionModel, LinearRegressionWithSGD}
import org.joda.time.DateTime

case class AlgorithmParams(
  iterations:        Int    = 20,
  regParam:          Double = 0.1,
  miniBatchFraction: Double = 1.0,
  stepSize:          Double = 0.001
) extends Params

class MultinomialLogisticRegressionAlgorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): Model = {
    val logisticRegression = new LogisticRegressionWithLBFGS()

    logisticRegression.setIntercept(true)
    logisticRegression.setValidateData(true)
    logisticRegression.setNumClasses(20) // Need to change when we have higher variations of demand levels
    logisticRegression.optimizer
      .setNumIterations(1000)
      .setConvergenceTol(0.01)
      .setUpdater(new SquaredL2Updater())
      .setRegParam(0.5)

// We can use the following sampling to reduce training set by sampling or increase training set by bootstrap
    val sample = preparedData.data.sample(true, 0.01).cache();
    sample.foreach(println)

    val logisticRegressionModel = logisticRegression.run(preparedData.data)
    println(logisticRegressionModel.intercept)
    println(logisticRegressionModel.weights)
    new Model(logisticRegressionModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class Model(mod: LogisticRegressionModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedTimeFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime), query.lat, query.lng))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.toFeaturesVector(normalizedTimeFeatureVector, locationClusterLabel)
    mod.predict((features))
  }
}

trait MyQuerySerializer extends CustomQuerySerializer {
  @transient override lazy val querySerializer = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
}

