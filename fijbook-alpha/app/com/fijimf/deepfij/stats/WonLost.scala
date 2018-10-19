package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Schedule, Team}
import play.api.Logger

import scala.util.{Failure, Success, Try}

final case class WonLost(s: Schedule, dates:List[LocalDate]) extends Analyzer[WonLostAccumulator] {
  val log = Logger(WonLost.getClass)
  val model: Model[WonLostAccumulator] = WonLost
  val data: Map[LocalDate, Map[Team, WonLostAccumulator]] = {
    log.info("Start creating WonLost")
    val zero = (Map.empty[Team, WonLostAccumulator], Map.empty[LocalDate, Map[Team, WonLostAccumulator]])
    Try {
      s.games
        .sortBy(_.date.toEpochDay)
        .foldLeft(zero)((tuple: (Map[Team, WonLostAccumulator], Map[LocalDate, Map[Team, WonLostAccumulator]]), game: Game) => {
          val (r0, byDate) = tuple
          val r1 = s.winner(game) match {
            case Some(t) => {
              r0 + (t -> r0.getOrElse(t, WonLostAccumulator()).addWin())
            }
            case None => r0
          }
          val r2 = s.loser(game) match {
            case Some(t) => {
              r1 + (t -> r1.getOrElse(t, WonLostAccumulator()).addLoss())
            }
            case None => r1
          }
          (r2, byDate + (game.date -> r2))
        })._2
    } match {
      case Success(x) =>
        val data = x.filterKeys(dates.contains(_))
        log.info(s"Computing WonLost succeeded with ${data.size} dates.")
        data
      case Failure(thr) =>
        log.info("Computing stat failed" + thr)
        zero._2
    }
  }
}

case object WonLost extends Model[WonLostAccumulator]{
  val stats: List[Stat[WonLostAccumulator]] = List(
    Stat[WonLostAccumulator]("Wins", "wins", 0, higherIsBetter = true, _.wins),
    Stat[WonLostAccumulator]("Losses", "losses", 0, higherIsBetter = false, _.losses),
    Stat[WonLostAccumulator]("Winning Streak", "wstreak", 0, higherIsBetter = true, _.winStreak),
    Stat[WonLostAccumulator]("Losing Streak", "lstreak", 0, higherIsBetter = false, _.lossStreak),
    Stat[WonLostAccumulator]("Winning Pct.", "wp", 0, higherIsBetter = true, _.winPct)
  )
  val key = "won-lost"
  val name: String = "Won Lost"
  val desc: String = "Some simple statistics compiled from team records."

  def create(s: Schedule, dates:List[LocalDate]):Option[WonLost] = {
    canCreateDates(s,dates) match {
      case Nil=>None
      case dates=>Some(WonLost(s,dates))
    }
  }
}
