package com.fijimf.deepfij.stats

case class ScoringAccumulator(pointsFor: List[Int] = List.empty[Int], pointsAgainst: List[Int] = List.empty[Int], margin: List[Int] = List.empty[Int], overUnder: List[Int] = List.empty) {
  def addGame(score: Int, oppScore: Int) = ScoringAccumulator(score :: pointsFor, oppScore :: pointsAgainst, (score - oppScore) :: margin, (score + oppScore) :: overUnder)
}
