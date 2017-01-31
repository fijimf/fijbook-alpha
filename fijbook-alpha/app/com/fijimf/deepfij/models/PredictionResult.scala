package com.fijimf.deepfij.models

case class PredictionResult(g: Game, p: GamePrediction, res: Option[Result]) {
  require(p.gameId == g.id && (p.favoriteId.isEmpty || p.favoriteId.get == g.homeTeamId || p.favoriteId.get == g.awayTeamId))

  def actualHomeSpread = res.map(r => r.homeScore - r.awayScore)

  def predictedHomeSpread = for {f <- p.favoriteId
                                 s <- p.spread} yield {
    if (f == g.homeTeamId) {
      s
    } else {
      -s
    }
  }

  def spreadError = for {actual <- actualHomeSpread
                         predicted <- predictedHomeSpread} yield {
    actual - predicted
  }

  def absSpreadError = spreadError.map(math.abs)


  def actualHomeWinner: Option[Int] = res.map(r => bool2Int(r.homeScore > r.awayScore))

  def predictedHomeWinner: Option[Double] = for {f <- p.favoriteId
  } yield {
    if (f == g.homeTeamId) {
      p.probability.getOrElse(1)
    } else {
      p.probability.map(1 - _).getOrElse(0)
    }
  }

  def winnerError = for {actual <- actualHomeWinner
                         predicted <- predictedHomeWinner} yield {
    math.abs(actual.toDouble - predicted)
  }

  def bool2Int(b: Boolean) = if (b) 1 else 0
}

object PredictionResult {
  def averageError(prs:List[PredictionResult]) = {
    val list = prs.filter(_.spreadError.isDefined)
    if (list.isEmpty) {
      None
    } else {
      Some(list.map(_.spreadError.get).sum / list.size)
    }
  }
}

