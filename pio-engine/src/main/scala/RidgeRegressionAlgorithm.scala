package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{CustomQuerySerializer, P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{LogisticRegressionModel, LogisticRegressionWithLBFGS}
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.feature.StandardScalerModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.optimization.SquaredL2Updater
import org.apache.spark.mllib.regression.{RidgeRegressionModel, RidgeRegressionWithSGD, LinearRegressionModel, LinearRegressionWithSGD}
import org.joda.time.DateTime

case class RidgeRegressionParams(
    iterations:        Int    = 1000,
    regParam:          Double = 0.5,
    miniBatchFraction: Double = 1.0,
    stepSize:          Double = 0.01
) extends Params

class RidgeRegressionAlgorithm(val ap: RidgeRegressionParams)
  extends P2LAlgorithm[PreparedData, RidgeRegressionAlgorithmModel, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): RidgeRegressionAlgorithmModel = {
//    val logisticRegression = new LogisticRegressionWithLBFGS()
    val ridgeRegressionWithSGD = new RidgeRegressionWithSGD();

    ridgeRegressionWithSGD.setIntercept(true)
    ridgeRegressionWithSGD.setValidateData(true)
    ridgeRegressionWithSGD.optimizer
      .setNumIterations(ap.iterations)
      .setMiniBatchFraction(ap.miniBatchFraction)
      .setStepSize(ap.stepSize)
      .setRegParam(ap.regParam)

// We can use the following sampling to reduce training set by sampling or increase training set by bootstrap
//    val sample = preparedData.data.sample(true, 0.01).cache();
//    sample.foreach(println)

    val ridgeRegressionModel = ridgeRegressionWithSGD.run(preparedData.data)
//    println(logisticRegressionModel.intercept)
//    println(logisticRegressionModel.weights)
    new RidgeRegressionAlgorithmModel(ridgeRegressionModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: RidgeRegressionAlgorithmModel, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class RidgeRegressionAlgorithmModel(mod: RidgeRegressionModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime),
      query.temperature, query.clear, query.fog, query.rain, query.snow, query.hail, query.thunder, query.tornado))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.combineFeatureVectors(normalizedFeatureVector, locationClusterLabel)
    mod.predict(features)
  }
}


