package com.fijimf.deepfij.models
case class WonLostRecord(won: Int = 0, lost: Int = 0) extends Ordering[WonLostRecord] {
  require(won >= 0 && lost >= 0)

  override def compare(x: WonLostRecord, y: WonLostRecord): Int = {
    (x.won - x.lost) - (y.won - y.lost) match {
      case 0 => x.won - y.won
      case i => i
    }
  }

  def pct: Option[Double] =
    won + lost match {
      case 0 => None
      case x => Some(won.toDouble / x.toDouble)
    }
}

