package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Schedule, Team}
import org.apache.commons.math3.stat.StatUtils
import play.api.Logger

import scala.util.{Failure, Success, Try}

final case class Scoring(s: Schedule, dates: List[LocalDate]) extends Analyzer[ScoringAccumulator] {
  val log = Logger(Scoring.getClass)
  val model: Model[ScoringAccumulator] = Scoring

  val zero = (Map.empty[Team, ScoringAccumulator], Map.empty[LocalDate, Map[Team, ScoringAccumulator]])

  val data: Map[LocalDate, Map[Team, ScoringAccumulator]] = {
    Try {
      processGames
    } match {
      case Success(x) =>
        val data = x.filterKeys(dates.contains(_))
        log.info(s"Computing Scoring succeeded with ${data.size} dates.")
        data
      case Failure(thr) =>
        log.error("Stat computation failed", thr)
        zero._2
    }
  }

  private def processGames: Map[LocalDate, Map[Team, ScoringAccumulator]] = {
    s.games.sortBy(_.date.toEpochDay).foldLeft(InnerAccumulator())((acc:InnerAccumulator, game: Game) => {
      s.resultMap.get(game.id) match {
          case Some(result) =>
            val acc1 = addGame(acc, game.homeTeamId, result.homeScore, result.awayScore)
            val acc2 = addGame(acc1, game.awayTeamId, result.awayScore, result.homeScore)

            acc2.copy(byDate = acc2.byDate+ (game.date -> acc2.runningTotals))
          case None => acc
        }
      }).byDate
  }

  private def addGame(acc: InnerAccumulator, teamId: Long, pointsFor: Int, pointsAgainst: Int):InnerAccumulator = {
    s.teamsMap.get(teamId) match {
      case Some(t) =>
        val data = acc.runningTotals.getOrElse(t, ScoringAccumulator()).addGame(pointsFor, pointsAgainst)
        acc.copy(runningTotals = acc.runningTotals + (t -> data))
      case None => acc
    }
  }

  final case class InnerAccumulator
  (
    runningTotals:Map[Team, ScoringAccumulator]=Map.empty[Team, ScoringAccumulator],
    byDate:Map[LocalDate, Map[Team, ScoringAccumulator]]=Map.empty[LocalDate,Map[Team,ScoringAccumulator]]
  )
}

case object Scoring extends Model[ScoringAccumulator] {
  val name: String = "Scoring"
  val desc: String = "Scoring model captures max, min, mean, and variance of points for, points against, scoring margin and combined score "
  val key: String = "scoring"

  val stats = List(
    Stat[ScoringAccumulator]("Mean Points For", "meanpf", 0, higherIsBetter = true, a => StatUtils.mean(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points For Variance", "varpf", 0, higherIsBetter = true, a => StatUtils.variance(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points For", "minpf", 0, higherIsBetter = true, a => StatUtils.min(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points For", "maxpf", 0, higherIsBetter = true, a => StatUtils.max(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Points Against", "meanpa", 0, higherIsBetter = false, a => StatUtils.mean(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points Against Variance", "varpa", 0, higherIsBetter = false, a => StatUtils.variance(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points Against", "minpa", 0, higherIsBetter = false, a => StatUtils.min(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points Against", "maxpa", 0, higherIsBetter = false, a => StatUtils.max(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Scoring Margin", "meanmrgp", 0, higherIsBetter = true, a => StatUtils.mean(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Scoring Margin Variance", "varmrg", 0, higherIsBetter = true, a => StatUtils.variance(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Scoring Margin", "minmrg", 0, higherIsBetter = true, a => StatUtils.min(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Scoring Margin", "maxmrg", 0, higherIsBetter = true, a => StatUtils.max(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Combined Score", "meanou", 0, higherIsBetter = true, a => StatUtils.mean(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Combined Score Variance", "varou", 0, higherIsBetter = true, a => StatUtils.variance(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Combined Score", "minou", 0, higherIsBetter = true, a => StatUtils.min(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Combined Score", "maxou", 0, higherIsBetter = true, a => StatUtils.max(a.overUnder.map(_.toDouble).toArray))
  )

  override def create(s: Schedule, ds: List[LocalDate]): Option[Scoring] = {
    canCreateDates(s,ds) match {
      case Nil=>None
      case dates=>Some(Scoring(s,dates))
    }
  }
}
