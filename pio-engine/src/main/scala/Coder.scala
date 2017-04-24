package detrevid.predictionio.loadforecasting

import scala.math.{cos, sin, Pi}

object Coder {
  def simpleCoding(value: Int, maxValue: Int): Array[Double] = {
    Array[Double](value.toDouble / maxValue.toDouble)
  }

  def circleCoding(value: Int, maxValue: Int): Array[Double] = {
    Array[Double](cos(2 * Pi * (value.toDouble / maxValue.toDouble)),
      sin(2 * Pi * (value.toDouble / maxValue.toDouble)))
  }

  def dummyCoding(value: Int, maxValue: Int): Array[Double] = {
    val arr = Array.fill[Double](maxValue)(0.0)
    if (value != 0) arr(value - 1) = 1.0
    arr
  }

  def effectCoding(value: Int, maxValue: Int): Array[Double] = {
    var arr = Array.fill[Double](maxValue)(0.0)
    if (value != 0) arr(value - 1) = 1.0
    else arr = arr map (_ => -1.0)
    arr
  }
}
