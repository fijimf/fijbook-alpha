package com.fijimf.deepfij.stats.predictor


case class LogisticRegression(rate: Double, maxIterations: Int) {

  def sigmoid(z: Double) = 1 / (1 + math.exp(-z))

  def train(data: List[(List[Double], Int)]): List[Double] = {
    0.to(maxIterations).foldLeft(List.empty[Double])((weights: List[Double], i: Int) => {
      data.foldLeft((0.0, weights))((accum: (Double, List[Double]), item: (List[Double], Int)) => {
        val xs = item._1
        val label = item._2;
        val zip = weights.zip(xs)
        val predicted = classify(zip);

        val newWeights = zip.map(t => t._1 + rate * (label - predicted) * t._2)

        (0, newWeights)
      })
      weights
    })
  }

  def classify(wxs: List[(Double, Double)]): Double = sigmoid(wxs.map(t => t._1 * t._2).sum)

}
