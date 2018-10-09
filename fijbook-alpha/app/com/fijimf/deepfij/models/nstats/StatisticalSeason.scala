package com.fijimf.deepfij.models.nstats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Schedule}


case class Obs(id: Long, seasonId: Long, teamId: Long, date: LocalDate, typeKey: String, value: Option[Double])

case class ObsType(id: Long, key: String, name: String, higherIsBetter: Boolean, defaultValue: Option[Double])

trait Statistic {
  def obs(seasonId: Long, teamId: Long, date: LocalDate): Option[Double]

  def series(seasonId: Long, teamId: Long): List[(LocalDate, Double)]

  def distribution(seasonId: Long, date: LocalDate): Map[Long, Double]
}

object Statistic {

}

trait Analyzer {
  def calculate(s: Schedule): Map[ObsType, List[Obs]]

  def completedGames(s: Schedule): List[(Game, Result)] = s.gameResults.flatMap {
    case (g, Some(r)) => Some((g, r))
    case (_, None) => None
  }.sortBy(_._1.date.toEpochDay)
}


//object WonLost extends Analyzer {
//  def calculate(s: Schedule): List[Obs] = {
//    val acc = completedGames(s).foldLeft(Accumulator.counter) {
//      case (a, (g, _)) => a.accumulate(g.homeTeamId, g.date).accumulate(g.awayTeamId, g.date)
//    }
//    acc.obs(s.season.id,"n-games",d=>Some(d))
//  }
//}


//object Accumulator {
//  val counter: Accumulator[Unit, Double] = Accumulator[Unit, Double]((_,x)=> x + 1.0, 0.0)
//}

//case class Accumulator[U,B](f: (U,B) => B, zB: B, m: Map[Long, B] = Map.empty, n: Map[LocalDate, Map[Long, B]] = Map.empty) {
//  def accumulate(t: Long, d: LocalDate): Accumulator[U, B] = {
//    val b1 = f(m.getOrElse(t, zB))
//    val m1 = m + (t -> b1)
//    val n1 = n + (d -> m1)
//    Accumulator(f, zB, m, n)
//  }
//
//  def obs(seasonId: Long, key: String, f: B => Option[Double]): List[Obs] = n.flatMap {
//    case (d, xs) => xs.map {
//      case (l: Long, b: B) =>
//        Obs(0L, seasonId, l, d, key, f(b))
//    }
//  }.toList
//}
//
