package com.fijimf.deepfij.models.nstats

import cats.implicits._

object HigherOrderAppenders {

  final case class ShorthandRecord(wins: List[Long] = List.empty, losses: List[Long] = List.empty) {
    def addWin(k: Long): ShorthandRecord = copy(wins = k :: wins)

    def addLoss(k: Long): ShorthandRecord = copy(losses = k :: losses)

    def w(): Double = wins.size.toDouble

    def l(): Double = losses.size.toDouble

    def ow(srf: Map[Long, ShorthandRecord]): Double = {
      wins.flatMap(k => srf.get(k).map(_.w())).sum + losses.flatMap(k => srf.get(k).map(_.w() - 1)).sum
    }

    def ol(srf: Map[Long, ShorthandRecord]): Double = {
      wins.flatMap(k => srf.get(k).map(_.l() - 1)).sum + losses.flatMap(k => srf.get(k).map(_.l())).sum
    }

    def oow(srf: Map[Long, ShorthandRecord]): Double = {
      wins.flatMap(k => srf.get(k).map(_.ow(srf))).sum + losses.flatMap(k => srf.get(k).map(_.ow(srf))).sum
    }

    def ool(srf: Map[Long, ShorthandRecord]): Double = {
      wins.flatMap(k => srf.get(k).map(_.ol(srf))).sum + losses.flatMap(k => srf.get(k).map(_.ol(srf))).sum
    }

    def wp: Double = if (l() + w() === 0) 0.0 else w() / (w() + l())

    def owp(srf: Map[Long, ShorthandRecord]): Double = if (ol(srf) + ow(srf) === 0) 0.0 else ow(srf) / (ow(srf) + ol(srf))

    def oowp(srf: Map[Long, ShorthandRecord]): Double = if (ool(srf) + oow(srf) === 0) 0.0 else oow(srf) / (oow(srf) + ool(srf))

    def rpi(srf: Map[Long, ShorthandRecord]): Double = (wp + owp(srf) + owp(srf) + oowp(srf)) / 4.0
  }


}
