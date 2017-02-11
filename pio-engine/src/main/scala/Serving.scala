package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.LServing

class Serving extends LServing[Query, PredictedResult] {

  override def serve(query: Query,
                     predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
