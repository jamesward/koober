package edu.cs5152.predictionio.demandforecasting
import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{CustomQuerySerializer, P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.mllib.feature.StandardScalerModel
import org.apache.spark.mllib.tree.GradientBoostedTrees
import org.apache.spark.mllib.tree.configuration.BoostingStrategy
import org.apache.spark.mllib.tree.model.GradientBoostedTreesModel
import org.joda.time.DateTime

/**
  * Created by YitingWang on 3/2/17.
  */

case class GBTreeParams(
  iterations:        Int = 100,
  maxDepth:          Int = 10
) extends Params

class AlgorithmGBTree(val ap: GBTreeParams)
  extends P2LAlgorithm[PreparedData, ModelGBTree, Query, PredictedResult] with MyQuerySerializer{

  override def train(sc: SparkContext, preparedData: PreparedData): ModelGBTree ={
    val boostingStrategy = BoostingStrategy.defaultParams("Regression")
    boostingStrategy.setNumIterations(ap.iterations)
    boostingStrategy.getTreeStrategy().setMaxDepth(ap.maxDepth)

    val gradientBoostedTreeModel = GradientBoostedTrees.train(preparedData.data, boostingStrategy)
    new ModelGBTree(gradientBoostedTreeModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  override def predict(model: ModelGBTree, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class ModelGBTree(mod: GradientBoostedTreesModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable { // will not be DateTime after changes
// to Preparator
@transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime),
      query.temperature, query.clear, query.fog, query.rain, query.snow, query.hail, query.thunder, query.tornado, query.heat,
      query.windchill, query.precipitation))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.combineFeatureVectors(normalizedFeatureVector, locationClusterLabel)
    mod.predict(features)
  }
}
