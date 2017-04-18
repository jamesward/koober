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

case class LogisticParams(
  iterations:        Int    = 4096,
  numClasses:          Int = 200,
  convergenceTol:      Double = 0.01,
  regParam:          Double = 0.5
) extends Params

class MultinomialLogisticRegressionAlgorithm(val ap: LogisticParams)
  extends P2LAlgorithm[PreparedData, LogisticModel, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): LogisticModel = {
    val logisticRegression = new LogisticRegressionWithLBFGS()

    logisticRegression.setIntercept(true)
    logisticRegression.setValidateData(true)
    logisticRegression.setNumClasses(ap.numClasses) // Need to change when we have higher variations of demand levels
    logisticRegression.optimizer
      .setNumIterations(ap.iterations)
      .setConvergenceTol(ap.convergenceTol)
      .setUpdater(new SquaredL2Updater())
      .setRegParam(ap.regParam)

// We can use the following sampling to reduce training set by sampling or increase training set by bootstrap
//    val sample = preparedData.data.sample(true, 0.01).cache();
//    sample.foreach(println)

    val logisticRegressionModel = logisticRegression.run(preparedData.data)
//    println(logisticRegressionModel.intercept)
//    println(logisticRegressionModel.weights)
    new LogisticModel(logisticRegressionModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: LogisticModel, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class LogisticModel(mod: LogisticRegressionModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime),
      query.temperature, query.clear, query.fog, query.rain, query.snow, query.hail, query.thunder, query.tornado))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.combineFeatureVectors(normalizedFeatureVector, locationClusterLabel)
    mod.predict(features)
  }
}


