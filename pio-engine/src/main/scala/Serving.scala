package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.LServing

class Serving extends LServing[Query, PredictedResult] {

  override def serve(query: Query,
                     predictedResults: Seq[PredictedResult]): PredictedResult = {
    println(predictedResults.length)
    println(predictedResults.head.demand)
    println(predictedResults.last.demand)
    var sumResult:Double = 0.0
    predictedResults.foreach(sumResult += _.demand)

    //val sumResult: Double = predictedResults.foldLeft(0.0){( acc: Double, pred: PredictedResult) => acc + pred.demand}
    println(sumResult)
    val meanResult: Double = sumResult / predictedResults.length
    new PredictedResult(meanResult)
  }
}
