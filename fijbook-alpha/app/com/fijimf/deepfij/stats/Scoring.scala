package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Schedule, Team}
import org.apache.commons.math3.stat.StatUtils

case class Scoring(s: Schedule) extends Analyzer[ScoringAccumulator] {

  val data: Map[LocalDate, Map[Team, ScoringAccumulator]] = {
    val zero = (Map.empty[Team, ScoringAccumulator], Map.empty[LocalDate, Map[Team, ScoringAccumulator]])
    s.games
      .sortBy(_.date.toEpochDay)
      .foldLeft(zero)((tuple: (Map[Team, ScoringAccumulator], Map[LocalDate, Map[Team, ScoringAccumulator]]), game: Game) => {
        val (r0, byDate) = tuple
        s.resultMap.get(game.id) match {
          case Some(result) =>
            val r1: Map[Team, ScoringAccumulator] = s.teamsMap.get(game.homeTeamId) match {
              case Some(t) =>
                r0 + (t -> r0.getOrElse(t, ScoringAccumulator()).addGame(result.homeScore, result.awayScore))
              case None => r0
            }
            val r2 = s.teamsMap.get(game.awayTeamId) match {
              case Some(t) =>
                r1 + (t -> r1.getOrElse(t, ScoringAccumulator()).addGame(result.awayScore, result.homeScore))
              case None => r1
            }
            (r2, byDate + (game.date -> r2))
          case None => (r0, byDate)
        }
      })._2
  }


  val stats = List(
    Stat[ScoringAccumulator]("Mean Points For", "meanpf", 0, higherIsBetter = true, a => StatUtils.mean(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points For Variance", "varpf", 0, higherIsBetter = true, a => StatUtils.variance(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points For", "minpf", 0, higherIsBetter = true, a => StatUtils.min(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points For", "maxpf", 0, higherIsBetter = true, a => StatUtils.max(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Points Against", "meanpa", 0, higherIsBetter = true, a => StatUtils.mean(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points Against Variance", "varpa", 0, higherIsBetter = true, a => StatUtils.variance(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points Against", "minpa", 0, higherIsBetter = true, a => StatUtils.min(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points Against", "maxpa", 0, higherIsBetter = true, a => StatUtils.max(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Scoring Margin", "meanmrgp", 0, higherIsBetter = true, a => StatUtils.mean(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Scoring Margin Variance", "varmrg", 0, higherIsBetter = true, a => StatUtils.variance(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Scoring Margin", "minmrg", 0, higherIsBetter = true, a => StatUtils.min(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Scoring Margin", "maxmrg", 0, higherIsBetter = true, a => StatUtils.max(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Combined Score", "meanou", 0, higherIsBetter = true, a => StatUtils.mean(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Combined Score Variance", "varou", 0, higherIsBetter = true, a => StatUtils.variance(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Combined Score", "minou", 0, higherIsBetter = true, a => StatUtils.min(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Combined Score", "maxou", 0, higherIsBetter = true, a => StatUtils.max(a.overUnder.map(_.toDouble).toArray))
  )
  override val name: String = "Scoring"
  override val desc: String = "Scoring model captures max, min, mean, and variance of points for, points against, scoring margin and combined score "
  override val key: String = "scoring"
}
