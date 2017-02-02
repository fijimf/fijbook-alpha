package com.fijimf.deepfij.models

import java.io.Serializable
import java.time.LocalDate

case class GprLine(date: String, homeTeam: String,homeKey:String, homeScore: Option[Int], awayTeam: String, awayKey:String, awayScore: Option[Int], favorite: Option[String], isFavoriteCorrect: Option[Boolean], spread: Option[Double], error: Option[Double])
case class GprCohort(gprls:List[GprLine], pctPredicted:Option[Double], pctRight:Option[Double], pctWrong:Option[Double], accuracy:Option[Double], avgSpreadErr:Option[Double], avgAbsSpreadErr:Option[Double])

object GprLine {
  def apply(g: Game, sch: Schedule): GprLine = {
    val d = g.date.toString
    val ht = sch.teamsMap.get(g.homeTeamId).map(_.name).getOrElse("")
    val hk = sch.teamsMap.get(g.homeTeamId).map(_.key).getOrElse("")
    val hs = sch.resultMap.get(g.id).map(_.homeScore)
    val at = sch.teamsMap.get(g.awayTeamId).map(_.name).getOrElse("")
    val ak = sch.teamsMap.get(g.awayTeamId).map(_.key).getOrElse("")
    val as = sch.resultMap.get(g.id).map(_.awayScore)
    val fav = sch.predictionMap.get(g.id).flatMap(_.favoriteId).flatMap(fi => sch.teamsMap.get(fi).map(_.name))
    val corr = for {
      h <- hs
      a <- as
      f <- fav} yield {
      (h > a && f == ht) || (h < a && f == at)
    }
    val spr = sch.predictionMap.get(g.id).flatMap(_.spread)
    val err = for {
      h <- hs
      a <- as
      f <- fav
      s <- spr
    } yield {
      if (f == ht) (a - h) - s else (h - a) - s
    }
    GprLine(d, ht, hk, hs, at, ak, as, fav, corr, spr, err)
  }
}

  object GprCohort {
    def apply(sch: Schedule): GprCohort = cohort(sch, sch.games)
    def apply(sch: Schedule, d:LocalDate): GprCohort = cohort(sch, sch.games.filter(_.date==d))

    def cohort(sch: Schedule, gs: List[Game]): GprCohort = {
      val gprls = gs.map(GprLine(_, sch))
      if (gprls.isEmpty) {
        GprCohort(gprls, None, None, None, None, None, None)
      } else {
        val predicted = gprls.filter(_.isFavoriteCorrect.isDefined)
        if (predicted.isEmpty) {
          GprCohort(gprls, Some(0), Some(0), Some(0), None, None, None)
        } else {
          val pctPredicted = Some(predicted.size.toDouble / gprls.size.toDouble)
          val pctRight = Some(predicted.count(_.isFavoriteCorrect.get).toDouble / gprls.size.toDouble)
          val pctWrong = Some(predicted.count(!_.isFavoriteCorrect.get).toDouble / gprls.size.toDouble)
          val accuracy = Some(predicted.count(!_.isFavoriteCorrect.get).toDouble / predicted.size.toDouble)
          val avgSpreadError = Some(predicted.map(_.error.getOrElse(0.0)).sum / predicted.size.toDouble)
          val avgAbsSpreadError = Some(predicted.map(p => math.abs(p.error.getOrElse(0.0))).sum / predicted.size.toDouble)
          GprCohort(gprls, pctPredicted, pctRight, pctWrong, accuracy, avgSpreadError, avgAbsSpreadError)
        }
      }
    }
  }



