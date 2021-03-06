package com.fijimf.deepfij.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.fijimf.deepfij.schedule.model.{Schedule, WonLostRecord}

import scala.util.Try

final case class TeamPredictionView(t:com.fijimf.deepfij.schedule.model.Team, ps:List[PredictionView]){
  def numPredicted:Int = ps.count(pv=> pv.isResultCorrect.isDefined)
  def numCorrect:Int = ps.count(pv=>pv.isResultCorrect.getOrElse(false))
  def pctCorrect:Double = if (numPredicted>0){
    numCorrect.doubleValue()/numPredicted.doubleValue()
  } else {
    0.0
  }

  val realizedRecord:WonLostRecord = {
    WonLostRecord(ps.count(_.isWinner(t)),ps.count(_.isLoser(t)))
  }

  def expectedRecord:WonLostRecord = {
val wl = recordDistribution.maxBy(_._2)._1
    WonLostRecord(wl.won+realizedRecord.won, wl.lost+realizedRecord.lost)
  }


  lazy val recordDistribution: List[(WonLostRecord, Double)] = {
    ps.filterNot(_.hasResult)
      .filter(_.pred.probability.isDefined)
      .foldLeft(
        Map(WonLostRecord()->1.0)
      )(
        (wls: Map[WonLostRecord, Double], pv: PredictionView) => {
          pv.pred.probability match {
            case Some(p)  =>
              val x = if (pv.isFavorite(t)) p else 1.0 - p
              val ww: Map[WonLostRecord, Double] = wls.map{case (wl, prob) => wl.copy(won=wl.won+1)->prob*x}
              val ll: Map[WonLostRecord, Double] = wls.map{case (wl, prob) => wl.copy(lost=wl.lost+1)->prob*(1-x)}
              val keys = ww.keySet++ll.keySet
              keys.toList.map(k=>{
                (ww.get(k),ll.get(k)) match {
                  case (Some(a), Some(b))=>k->(a+b)
                  case (Some(a), None)=>k->a
                  case (None, Some(b))=>k->b
                  case _ => throw new IllegalStateException("Yikes")
                }
              }).toMap
            case _ => wls
          }
    }).toList.sortBy(_._1.won)
  }
}

final case class PredictionView(s: Schedule, pred: XPrediction) {
  require(s.gameMap.contains(pred.gameId), s"Predicted game ${pred.gameId} unknown in schedule ${s.season.year}")
  val game: com.fijimf.deepfij.schedule.model.Game = s.gameMap(pred.gameId)
  require(s.teamsMap.contains(game.homeTeamId), s"Home team ${game.homeTeamId} unknown in schedule ${s.season.year}")
  require(s.teamsMap.contains(game.awayTeamId), s"Away team ${game.awayTeamId} unknown in schedule ${s.season.year}")
  if (pred.favoriteId.isDefined) {
    require(game.homeTeamId === pred.favoriteId.getOrElse(-1) || game.awayTeamId === pred.favoriteId.getOrElse(-1), s"Favorite team ${pred.favoriteId.getOrElse(-1)} unknown in game ${pred.gameId}")
  }

  def result: Option[com.fijimf.deepfij.schedule.model.Result] = s.resultMap.get(game.id)

  def homeTeam: com.fijimf.deepfij.schedule.model.Team = s.teamsMap(game.homeTeamId)

  def homeProb: Option[Double] = pred.probability.flatMap(x => {
    pred.favoriteId match {
      case Some(k) if k === game.homeTeamId => Some(x)
      case Some(k) if k === game.awayTeamId => Some(1 - x)
      case _ => None
    }
  })

  def awayTeam: com.fijimf.deepfij.schedule.model.Team = s.teamsMap(game.awayTeamId)

  def awayProb: Option[Double] = pred.probability.flatMap(x => {
    pred.favoriteId match {
      case Some(k) if k === game.homeTeamId => Some(1 - x)
      case Some(k) if k === game.awayTeamId => Some(x)
      case _ => None
    }
  })

  def isWinner(t: com.fijimf.deepfij.schedule.model.Team): Boolean = {
    s.isWinner(t, game)
  }

  def isLoser(t: com.fijimf.deepfij.schedule.model.Team): Boolean = {
    s.isLoser(t, game)
  }

  def date: LocalDate = game.date

  def dateStr: String = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  def hasResult: Boolean = s.resultMap.contains(game.id)

  def isResultCorrect: Option[Boolean] = s.winner(game).flatMap(w => pred.favoriteId.map(f => f === w.id))

  def isFavorite(t: com.fijimf.deepfij.schedule.model.Team): Boolean = pred.favoriteId.contains(t.id)

  def isUnderdog(t: com.fijimf.deepfij.schedule.model.Team): Boolean = (game.homeTeamId === t.id || game.awayTeamId === t.id) && !isFavorite(t)

  def odds(t:com.fijimf.deepfij.schedule.model.Team):Option[Double] = if (isFavorite(t)) {
    pred.odds
  } else if (isUnderdog(t)){
    pred.odds.map(x=>1/x)
  } else {
    None
  }
}

object PredictionView {
  def create(s: Schedule, pred: XPrediction): Option[PredictionView] = {
    Try {
      PredictionView(s, pred)
    }.toOption
  }
}
