package detrevid.predictionio.loadforecasting

import org.apache.predictionio.controller.PPreparator
import org.apache.predictionio.controller.SanityCheck

import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import java.util.{Calendar, TimeZone}

class PreparedData (
                     val eventTimes: Array[Long],
                     val data: RDD[(Int, LabeledPoint)]
                   ) extends Serializable with SanityCheck {

  override def sanityCheck(): Unit = {
    require(data.take(1).nonEmpty, s"data cannot be empty!")
  }
}

class Preparator extends PPreparator[TrainingData, PreparedData] {

  @transient lazy val logger = Logger[this.type]

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val eventTimes = trainingData.data map { _.eventTime } distinct() collect()

    val data = trainingData.data map{
      ev => (ev.eventTime, 1)
    } reduceByKey(_+_) cache()

    new PreparedData(eventTimes, data)
  }
}

object Preparator {

  @transient lazy val logger = Logger[this.type]
  @transient lazy val timeZone = TimeZone.getTimeZone("America/Los_Angeles")

  def getLocalTime(timestamp: Long, timeZone: TimeZone): Calendar = {
    val timeInMs: Long = timestamp * 1000
    val cal = Calendar.getInstance()
    val utcTimeInMs: Long = timeInMs - cal.getTimeZone.getOffset(timeInMs)
    val localTimeInMs: Long = utcTimeInMs + timeZone.getOffset(utcTimeInMs)
    cal.setTimeInMillis(localTimeInMs)
    cal
  }

  def getSeason(month: Int): Int = {
    val season = Array(0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0)
    season(month)
  }

  def getSeason(cal: Calendar): Int = getSeason(cal.get(Calendar.MONTH))

  def isWeekDay(day: Int): Int = if (day == Calendar.SATURDAY - 1 || day == Calendar.SUNDAY - 1) 0 else 1

  def toFeaturesVector(eventTime: Long): Vector = {
    toFeaturesVector(eventTime, Coder.effectCoding)
  }

  def toFeaturesVector(circuitId: Int, timestamp: Long,
                       coding: (Int, Int) => Array[Double]): Vector = {
    val cal = getLocalTime(timestamp, timeZone)

    val (maxHour, maxDayWeek, maxDayMonth, maxMonth, maxSeason) = (23, 6, 30, 11, 3)
    val (hour, dayWeek, dayMonth, month) =
      (cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.DAY_OF_WEEK) - 1,
        cal.get(Calendar.DAY_OF_MONTH) - 1,
        cal.get(Calendar.MONTH))
    val (weekday, season) = (isWeekDay(dayWeek), getSeason(cal))

    val (hourC, dayWeekC, isWeekdayC, dayMonthC, monthC, seasonC, hourWeekC, dayWeekDayMothC, dayMonthMothC) =
      (coding(hour, maxHour),
        coding(dayWeek, maxDayWeek),
        coding(weekday, 1),
        coding(dayMonth, maxDayMonth),
        coding(month, maxMonth),
        coding(season, maxSeason),
        coding((hour + 1) * (dayWeek + 1) - 1, (maxHour + 1) * (maxDayWeek + 1) - 1),
        coding((dayMonth + 1) * (dayWeek + 1) - 1, (maxDayMonth + 1) * (maxDayWeek + 1) - 1),
        coding((dayMonth + 1) * (month + 1) - 1, (maxDayMonth + 1) * (maxMonth + 1) - 1))

    val features =
      hourC ++
        dayWeekC ++
        dayMonthC ++
        isWeekdayC ++
        seasonC ++
        monthC ++
        hourWeekC ++
        dayWeekDayMothC ++
        dayMonthMothC

    Vectors.dense(features)
  }
}

