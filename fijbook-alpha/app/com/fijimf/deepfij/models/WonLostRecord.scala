package com.fijimf.deepfij.models

object WonLostRecord {
   def compare(x: WonLostRecord, y: WonLostRecord): Int = {
    (x.won - x.lost) - (y.won - y.lost) match {
      case 0 => x.won - y.won
      case i => i
    }
  }
}

final case class WonLostRecord(won: Int = 0, lost: Int = 0) extends Ordering[WonLostRecord] {
  require(won >= 0 && lost >= 0)

  override def compare(x: WonLostRecord, y: WonLostRecord): Int = WonLostRecord.compare(x,y)

  def pct: Option[Double] =
    won + lost match {
      case 0 => None
      case x => Some(won.toDouble / x.toDouble)
    }
}

