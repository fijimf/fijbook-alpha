package com.fijimf.deepfij.models.nstats

import cats.implicits._
import com.fijimf.deepfij.schedule.model.Schedule

object HigherOrderCounters {

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

  trait base extends Analysis[Map[Long, ShorthandRecord]] {

    override def zero(s:Schedule): Map[Long, ShorthandRecord] = Map.empty[Long, ShorthandRecord]

    override def update(os: Option[Scoreboard], b: Map[Long, ShorthandRecord]): Map[Long, ShorthandRecord] = os match {
      case Some(sb) =>
        sb.gs.foldLeft(b) {
          case (map, (game, result)) =>
            val (winK, lossK) = if (result.homeScore > result.awayScore) {
              (game.homeTeamId, game.awayTeamId)
            } else {
              (game.awayTeamId, game.homeTeamId)
            }
            map + (winK -> map.getOrElse(winK, ShorthandRecord()).addWin(lossK)) + (lossK -> map.getOrElse(lossK, ShorthandRecord()).addLoss(winK))
        }


      case None => b
    }

    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double]
  }

  object wins extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.w())

    override def key: String = "ho-wins"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object losses extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.l())

    override def key: String = "ho-losses"

    override def higherIsBetter: Boolean = true
    override def bounds=(0,Double.PositiveInfinity)
  }

  object winPct extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(s => {
      s.wp
    })

    override def key: String = "wp"

    override def higherIsBetter: Boolean = true

    override def bounds=(0.0,1.0)

  }

  object oppWins extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.ow(b))

    override def key: String = "opp-wins"

    override def higherIsBetter: Boolean = true
    override def bounds=(0,Double.PositiveInfinity)
  }

  object oppLosses extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.ol(b))

    override def key: String = "opp-losses"

    override def higherIsBetter: Boolean = true
    override def bounds=(0,Double.PositiveInfinity)
  }

  object oppWinPct extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(s => {
      s.owp(b)
    })

    override def key: String = "opp-wp"

    override def higherIsBetter: Boolean = true
    override def bounds=(0.0,1.0)
  }

  object oppOppWins extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.oow(b))

    override def key: String = "opp-opp-wins"

    override def higherIsBetter: Boolean = true
    override def bounds=(0,Double.PositiveInfinity)
  }

  object oppOppLosses extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(_.ool(b))

    override def key: String = "opp-opp-losses"

    override def higherIsBetter: Boolean = false
    override def bounds=(0,Double.PositiveInfinity)
  }

  object oppOppWinPct extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(s => {
      s.oowp(b)
    })

    override def key: String = "opp-opp-wp"

    override def higherIsBetter: Boolean = true
    override def bounds=(0.0,1.0)
  }


  object rpi extends base {
    override def extract(b: Map[Long, ShorthandRecord]): Map[Long, Double] = b.mapValues(s => {
      s.rpi(b)
    })

    override def key: String = "rpi"

    override def higherIsBetter: Boolean = true
    override def bounds=(0.0,1.0)
  }


}
