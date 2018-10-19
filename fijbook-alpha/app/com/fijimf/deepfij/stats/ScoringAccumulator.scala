package com.fijimf.deepfij.stats

final case class ScoringAccumulator
(
  pointsFor: List[Int] = List.empty[Int],
  pointsAgainst: List[Int] = List.empty[Int],
  margin: List[Int] = List.empty[Int],
  overUnder: List[Int] = List.empty
) {
  def addGame(score: Int, oppScore: Int) = ScoringAccumulator(
    pointsFor = score :: pointsFor,
    pointsAgainst = oppScore :: pointsAgainst,
    margin = (score - oppScore) :: margin,
    overUnder = (score + oppScore) :: overUnder
  )
}
