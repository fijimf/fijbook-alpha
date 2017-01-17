package com.fijimf.deepfij.stats

case class RpiAccumulator(wins: Int = 0, losses: Int = 0, oppWins: Int = 0, oppLosses: Int = 0, oppOppWins: Int = 0, oppOppLosses: Int = 0) {
  def addWin(): RpiAccumulator = copy(wins = wins + 1)

  def addLoss(): RpiAccumulator = copy(losses = losses + 1)

  def addOppWins(n: Int): RpiAccumulator = copy(oppWins = oppWins + n)

  def addOppLosses(n: Int): RpiAccumulator = copy(oppLosses = oppLosses + n)

  def addOppOppWins(n: Int): RpiAccumulator = copy(oppOppWins = oppOppWins + n)

  def addOppOppLosses(n: Int): RpiAccumulator = copy(oppOppLosses = oppOppLosses + n)

  val winPct: Double = if (wins + losses == 0) 0.0 else wins.toDouble / (wins + losses).toDouble
  val oppWinPct: Double = if (oppWins + oppOppLosses == 0) 0.0 else oppWins.toDouble / (oppWins + oppOppLosses).toDouble
  val oppOppWinPct: Double = if (oppOppWins + oppOppLosses == 0) 0.0 else oppOppWins.toDouble / (oppOppWins + oppOppLosses).toDouble

  def rpi(x: Int = 1, y: Int = 1, z: Int = 1): Double = (x * winPct + y * oppWinPct + z * oppOppWinPct) / (x + y + z)

  def rpig(x: Int = 1, y: Int = 1, z: Int = 1): Double = math.pow(math.pow(winPct, x) * math.pow(oppWinPct, y) * math.pow(oppOppWinPct, z), 1.0/(x + y + z).toDouble)
}
