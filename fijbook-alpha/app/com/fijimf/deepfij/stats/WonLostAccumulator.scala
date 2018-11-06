package com.fijimf.deepfij.stats

final case class WonLostAccumulator(wins: Int = 0, losses: Int = 0, winStreak: Int = 0, lossStreak: Int = 0) {
  def addWin() = WonLostAccumulator(wins + 1, losses, winStreak + 1, 0)

  def addLoss() = WonLostAccumulator(wins, losses + 1, 0, lossStreak + 1)

  val winPct: Double = if (wins + losses == 0) 0.0 else wins.toDouble / (wins + losses).toDouble
}
