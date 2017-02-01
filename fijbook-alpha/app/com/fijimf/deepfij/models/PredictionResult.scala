package com.fijimf.deepfij.models

import java.io.Serializable

case class GprLine(date: String, homeTeam: String, homeScore: Option[Int], awayTeam: String, awayScore: Option[Int], favorite: Option[String], isFavoriteCorrect: Option[Boolean], spread: Option[Double], error: Option[Double])
case class GprCohort(gprls:List[GprLine], pctPredicted:Option[Double], pctRight:Option[Double], pctWrong:Option[Double], accuracy:Option[Double], avgSpreadErr:Option[Double], avgAbsSpreadErr:Option[Double])

object GprLine {
  def apply(g: Game, sch: Schedule): GprLine = {
    val d = g.date.toString
    val ht = sch.teamsMap.get(g.homeTeamId).map(_.name).getOrElse( "")
    val hs = sch.resultMap.get(g.id).map(_.homeScore)
    val at = sch.teamsMap.get(g.awayTeamId).map(_.name).getOrElse( "")
    val as = sch.resultMap.get(g.id).map(_.awayScore)
    val fav= sch.predictionMap.get(g.id).flatMap(_.favoriteId).flatMap(fi => sch.teamsMap.get(fi).map(_.name))
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
    GprLine(d,ht,hs,at,as,fav,corr,spr,err)
  }

  def cohort(sch:Schedule, gs:List[Game]):GprCohort = {
    val gprls = gs.map(GprLine(_, sch))
    if (gprls.isEmpty) {
      GprCohort(gprls, None, None, None, None, None, None)
    } else {
      val predicted = gprls.filter(_.favorite.isDefined)
     if (predicted.isEmpty){
       GprCohort(gprls, Some(0), Some(0), Some(0), None, None, None)
     } else {
       val pctPredicted = Some(predicted.size.toDouble / gprls.size.toDouble)
       val pctRight = Some(predicted.count(_.isFavoriteCorrect.get).toDouble / gprls.size.toDouble)
       val pctWrong = Some(predicted.count(!_.isFavoriteCorrect.get).toDouble / gprls.size.toDouble)
       val accuracy = Some(predicted.count(!_.isFavoriteCorrect.get).toDouble / predicted.size.toDouble)
       val avgSpreadError = Some(predicted.map(_.spread.getOrElse(0.0)).sum/predicted.size.toDouble)
       val avgAbsSpreadError = Some(predicted.map(p=>math.abs(p.spread.getOrElse(0.0))).sum/predicted.size.toDouble)
       GprCohort(gprls, pctPredicted, pctRight, pctWrong, accuracy, avgSpreadError, avgAbsSpreadError)
     }
    }
  }
}


