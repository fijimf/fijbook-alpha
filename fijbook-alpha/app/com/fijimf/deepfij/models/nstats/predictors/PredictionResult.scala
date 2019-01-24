package com.fijimf.deepfij.models.nstats.predictors

import cats.implicits._
import com.fijimf.deepfij.models.{Game, Result, XPrediction}

case class PredictionResult(g: Game, r: Option[Result], xp: Option[XPrediction]) {
  val favoriteCorrect: Option[Boolean] = for {
    result <- r
    prediction <- xp
    fav <- prediction.favoriteId
  } yield {
    if (fav === g.homeTeamId) {
      result.homeScore > result.awayScore
    } else {
      result.homeScore < result.awayScore
    }
  }

  val favoriteIncorrect: Option[Boolean] = favoriteCorrect.map(! _)

  val spreadError: Option[Double] = for {
    result <- r
    prediction <- xp
    fav <- prediction.favoriteId
    spr <- prediction.spread
  } yield {
    if (g.homeTeamId === fav) {
      spr - (result.homeScore - result.awayScore)
    } else {
      spr - (result.awayScore - result.homeScore)
    }
  }

  val absSpreadError: Option[Double] =spreadError.map(math.abs)

  val probabilityError: Option[Double] =for{
    ok<-favoriteCorrect
    prediction<-xp
    prob<-prediction.probability
  } yield{
    if (ok) {
      -math.log(prob)/math.log(2.0)
    } else {
      -math.log(1.0-prob)/math.log(2.0)
    }
  }

  def homeId: Long =g.homeTeamId
  def homeScore: Option[Int] =r.map(_.homeScore)
  def awayId: Long =g.awayTeamId
  def awayScore: Option[Int] =r.map(_.awayScore)
  def favorite: Option[Long] =xp.flatMap(_.favoriteId)
  def spread: Option[Double] =xp.flatMap(_.spread)
  def probability: Option[Double] =xp.flatMap(_.probability)
}

object PredictionResult {
  def numPredicted(obs:Seq[PredictionResult]): Int ={
    obs.count(_.favorite.isDefined)
  }
  def numCorrect(obs:Seq[PredictionResult]): Int ={
    obs.count(_.favoriteCorrect.getOrElse(false))
  }
  def numIncorrect( obs:Seq[PredictionResult]): Int ={
    obs.count(_.favoriteIncorrect.getOrElse(false))
  }

  def accuracy(obs:Seq[PredictionResult]): Double ={
    val correct = numCorrect(obs)
    val incorrect = numIncorrect(obs)
    if (correct+incorrect===0){
      0.0
    } else {
      correct.toDouble/(correct+incorrect)
    }
  }

  def meanSpreadError(obs:Seq[PredictionResult]): Double ={
    val num = obs.count(_.spreadError.isDefined)
    if (num===0){
      0.0
    } else {
      obs.flatMap(_.spreadError).sum/num
    }
  }
  def meanAbsoluteSpreadError(obs:Seq[PredictionResult]): Double ={
    val num = obs.count(_.absSpreadError.isDefined)
    if (num===0){
      0.0
    } else {

      obs.flatMap(_.absSpreadError).sum/num
    }
  }
  def meanProbError(obs:Seq[PredictionResult]): Double ={
    val num = obs.count(_.probabilityError.isDefined)
    if (num===0){
      0.0
    } else {
      obs.flatMap(_.probabilityError).sum/num
    }
  }
}
