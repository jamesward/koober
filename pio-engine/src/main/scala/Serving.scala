package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.LServing

class Serving extends LServing[Query, PredictedResult] {

  override def serve(query: Query,
                     predictedResults: Seq[PredictedResult]): PredictedResult = {

    val algorithms : Map[String, Double] = predictedResults.foldLeft(Map.empty[String, Double]) {
      (acc: Map[String, Double], pred: PredictedResult) =>
        (acc.keySet ++ pred.algorithms.keySet).map(i=>
          (i, acc.getOrElse(i, 0.0) + pred.algorithms.getOrElse(i,0.0))).toMap
    }

    val demand = predictedResults.foldLeft(0.0) {
      (acc : Double, pred: PredictedResult) =>
        acc + pred.demand
    }

    new PredictedResult(demand / predictedResults.length, algorithms)
  }
}
