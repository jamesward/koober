package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.PPreparator
import org.apache.predictionio.controller.SanityCheck
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
    val eventTimes = trainingData.data map { _.eventTime } distinct () collect ()

    val timeMap = trainingData.data map { ev =>
      (ev.eventTime, normalize(ev.eventTime, "minuteOfYear"))}

    val countMap = timeMap.keys map { eventTime =>
      (timeMap.lookup(eventTime), 1)
    } reduceByKey (_ + _)

    val data = trainingData.data map { ev =>
      LabeledPoint(Row.fromSeq(countMap.lookup(timeMap.lookup(ev.eventTime))).getInt(0).toDouble, //This long and complicated line basically just get the demand out of the countMap
        Preparator.toFeaturesVector(ev.eventTime))
    } cache ()

    new PreparedData(data)
  }

  /**
    * Based on different metrics, try to normalize the event time into a double so we can calculate demand
    * @param eventTime
    * @param metric one of
    * @return
    */
  def normalize(eventTime: DateTime, metric: String): Int ={
    val minuteOfMonth   = (_:Any) => eventTime.dayOfMonth().get()*24*60 + eventTime.hourOfDay().get()*60 + eventTime.minuteOfDay().get()
    val halfHourOfMonth = (_:Any) => eventTime.dayOfMonth().get()*24*2 + eventTime.hourOfDay().get()*2 + eventTime.minuteOfDay().get()/30
    val hourOfMonth     = (_:Any) => eventTime.dayOfMonth().get()*24 + eventTime.hourOfDay().get()
    metric match{
      case "hourOfYear"     => eventTime.monthOfYear().get()*31*24 + hourOfMonth(Nil)
      case "halfHourOfYear" => eventTime.monthOfYear().get()*31*24*2 + halfHourOfMonth(Nil)
      case "minuteOfYear"   => eventTime.monthOfYear().get()*31*24*60 + minuteOfMonth(Nil)
      case "hourOfMonth"    => hourOfMonth(Nil)
      case "halfHourOfYear" => halfHourOfMonth(Nil)
      case "minuteOfMonth"  => minuteOfMonth(Nil)
      case "hourOfWeek"     => eventTime.dayOfWeek().get()*7*24 + eventTime.hourOfDay().get()
      case "halfHourOfWeek" => eventTime.dayOfWeek().get()*7*24*2 + eventTime.hourOfDay().get()*2 + eventTime.minuteOfDay().get()/30
      case "minuteOfWeek"   => eventTime.dayOfWeek().get()*7*24*60 + eventTime.hourOfDay().get()*60 + eventTime.minuteOfDay().get()

    }
  }
}

object Preparator {

  @transient lazy val logger = Logger[this.type]

  def toFeaturesVector(eventTime: DateTime): Vector = {
    Vectors.dense(
      Array(
        eventTime.dayOfWeek().get().toDouble,
        eventTime.dayOfMonth().get().toDouble,
        eventTime.minuteOfDay().get().toDouble,
        eventTime.monthOfYear().get().toDouble
      )) //will be changed when Preparator is properly implemented
  }
}
