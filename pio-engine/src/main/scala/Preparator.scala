package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.PPreparator
import org.apache.predictionio.controller.SanityCheck
import edu.cs5152.predictionio.demandforecasting.KooberUtil
import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import java.util.{Calendar, TimeZone}

import org.joda.time.DateTime

class PreparedData(
    val data: RDD[LabeledPoint]
) extends Serializable
    with SanityCheck {


  override def sanityCheck(): Unit = {
    require(data.take(1).nonEmpty, s"data cannot be empty!")
  }
}

class Preparator extends PPreparator[TrainingData, PreparedData] {

  @transient lazy val logger = Logger[this.type]

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(KooberUtil.prepareDemand(trainingData.data))
  }
}


