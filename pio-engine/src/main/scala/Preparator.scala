package detrevid.predictionio.loadforecasting

import org.apache.predictionio.controller.PPreparator
import org.apache.predictionio.controller.SanityCheck

import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row

import java.util.{Calendar, TimeZone}

class PreparedData (
                     val eventTimes: Array[Long],
                     val data: RDD[(Long, LabeledPoint)]
                   ) extends Serializable with SanityCheck {

  override def sanityCheck(): Unit = {
    require(data.take(1).nonEmpty, s"data cannot be empty!")
  }
}

class Preparator extends PPreparator[TrainingData, PreparedData] {

  @transient lazy val logger = Logger[this.type]

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val eventTimes = trainingData.data map { _.eventTime } distinct() collect()

    val countMap = trainingData.data map{
      ev => (ev.eventTime, 1)
    } reduceByKey(_+_) 

    val data = countMap.keys map {
      eventTime => (eventTime, LabeledPoint(Row.fromSeq(countMap.lookup(eventTime)).getInt(0).toDouble, Preparator.toFeaturesVector(eventTime)))
    } cache()

    new PreparedData(eventTimes, data)
  }
}

object Preparator {

  @transient lazy val logger = Logger[this.type]

  def toFeaturesVector(eventTime: Long): Vector = {
    Vectors.dense(eventTime)
  }
}

