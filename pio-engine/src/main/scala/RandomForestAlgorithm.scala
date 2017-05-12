package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.feature.StandardScalerModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.joda.time.DateTime

case class ForestParams (
                          numTrees: Int = 10,
                          maxDepth: Int = 10,
                          numBins:  Int = 32
                        ) extends Params

class RandomForestAlgorithm(val fp: ForestParams)
  extends P2LAlgorithm[PreparedData, ForestModel, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): ForestModel = {
    val randomForestModel = RandomForest.trainRegressor(preparedData.data, Map[Int,Int](), fp.numTrees, "auto",
      "variance", fp.maxDepth, fp.numBins);
    new ForestModel(randomForestModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: ForestModel, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label, Map("randomForest" -> label))
  }
}

class ForestModel(mod: RandomForestModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime),
      query.temperature, query.clear, query.fog, query.rain, query.snow, query.hail, query.thunder, query.tornado))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.combineFeatureVectors(normalizedFeatureVector, locationClusterLabel)
    mod.predict(features)
  }
}


