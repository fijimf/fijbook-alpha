package com.fijimf.deepfij.models

import java.time.LocalDate

import play.api.libs.json._

case class GprLine(date: String, homeTeam: String, homeKey: String, homeScore: Option[Int], awayTeam: String, awayKey: String, awayScore: Option[Int], favorite: Option[String], isFavoriteCorrect: Option[Boolean], spread: Option[Double], error: Option[Double]) {
  def toJson = {
    val hs: Int = homeScore.getOrElse(-1)
    val as: Int = awayScore.getOrElse(-1)
    val sp: Double = spread.getOrElse(Double.NaN)
    val er: Double = error.getOrElse(Double.NaN)
    val act = sp + er
    JsObject(Seq(
      "date" -> JsString(date),
      "homeTeam" -> JsString(homeTeam),
      "homeKey" -> JsString(homeKey),
      "homeScore" -> JsNumber(hs),
      "awayTeam" -> JsString(awayTeam),
      "awayKey" -> JsString(awayKey),
      "awayScore" -> JsNumber(as),
      "favorite" -> JsString(favorite.getOrElse("")),
      "isFavoriteCorrect" -> JsString(if(isFavoriteCorrect.getOrElse(false)) "Yes" else "No"),
      "spread" -> JsNumber(sp),
      "actual" -> JsNumber(act),
      "error" -> JsNumber(er),
      "absError" -> JsNumber(math.abs(er))
    ))
  }
}

case class GprCohort(gprls: List[GprLine], pctPredicted: Option[Double], pctRight: Option[Double], pctWrong: Option[Double], accuracy: Option[Double], avgSpreadErr: Option[Double], avgAbsSpreadErr: Option[Double]) {
  def toJson = JsArray(gprls.map(_.toJson))



}

object GprLine {
  def apply(g: Game, sch: Schedule): GprLine = {
    val date = g.date.toString
    val homeTeam = sch.teamsMap.get(g.homeTeamId).map(_.name).getOrElse("")
    val homeKey = sch.teamsMap.get(g.homeTeamId).map(_.key).getOrElse("")
    val homeScore = sch.resultMap.get(g.id).map(_.homeScore)
    val awayTeam = sch.teamsMap.get(g.awayTeamId).map(_.name).getOrElse("")
    val awayKey = sch.teamsMap.get(g.awayTeamId).map(_.key).getOrElse("")
    val awayScore = sch.resultMap.get(g.id).map(_.awayScore)
    val fav = sch.predictionMap.get(g.id).flatMap(_.favoriteId).flatMap(fi => sch.teamsMap.get(fi).map(_.name))
    val correct = for {
      h <- homeScore
      a <- awayScore
      f <- fav} yield {
      (h > a && f == homeTeam) || (h < a && f == awayTeam)
    }
    val spread = sch.predictionMap.get(g.id).flatMap(_.spread)
    val error = for {
      h <- homeScore
      a <- awayScore
      f <- fav
      s <- spread
    } yield {
      if (f == homeTeam) (a - h) - s else (h - a) - s
    }
    GprLine(date, homeTeam, homeKey, homeScore, awayTeam, awayKey, awayScore, fav, correct, spread, error)
  }
}

object GprCohort {
  def apply(sch: Schedule): GprCohort = cohort(sch, sch.games)

  def apply(sch: Schedule, d: LocalDate): GprCohort = cohort(sch, sch.games.filter(_.date == d))

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
        val accuracy = Some(predicted.count(_.isFavoriteCorrect.get).toDouble / predicted.size.toDouble)
        val avgSpreadError = Some(predicted.map(_.error.getOrElse(0.0)).sum / predicted.size.toDouble)
        val avgAbsSpreadError = Some(predicted.map(p => math.abs(p.error.getOrElse(0.0))).sum / predicted.size.toDouble)
        GprCohort(gprls, pctPredicted, pctRight, pctWrong, accuracy, avgSpreadError, avgAbsSpreadError)
      }
    }
  }
}



