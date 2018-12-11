package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction}
import smile.classification._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BaselineLogisticPredictor(kernel: Option[LogisticRegression]) extends ModelEngine[LogisticRegression] {

  def featureExtractor(s: Schedule, ss: StatValueDAO): Game => Future[Option[Map[String, Double]]] = {
    g:Game=> {
      for {
        snap <- ss.findXStatsSnapshot(g.seasonId, g.date, "ols")
      } yield {
        for {
          h <- snap.find(_.teamId === g.homeTeamId).flatMap(_.zScore)
          a <- snap.find(_.teamId === g.awayTeamId).flatMap(_.zScore)
        } yield {
          Map(
            "ols-z-diff" -> h
          )
        }
      }
    }
  }

  override def predict(s: Schedule, ss: StatValueDAO): Game => Future[Option[XPrediction]] = {
    val f = featureExtractor(s, ss)
    val now = LocalDate.now()
    kernel match {
      case Some(k) =>
        g: Game => {
          for {
            features <- f(g)
          } yield {
            features.flatMap(_.get("ols-z-diff")).map(x => {
              val pp = Array[Double](Double.NaN, Double.NaN)
              val p = k.predict(Array(x), pp)
              if (p === 1) {
                XPrediction(0L, g.id, 0L, now, "", Some(g.homeTeamId), Some(pp(0)), None, None)
              } else {
                XPrediction(0L, g.id, 0L, now, "", Some(g.awayTeamId), Some(pp(1)), None, None)
              }
            })
          }
        }
      case _ => g: Game => Future.successful(None)
    }
  }

  override def train(ss: List[Schedule], sx: StatValueDAO): Future[ModelEngine[LogisticRegression]] = {

    val futureTuples = Future.sequence(ss.map(s => {
      val f: Game => Future[Option[Map[String, Double]]] = featureExtractor(s, sx)
      val c: (Game, Result) => Future[Option[Double]] = categoryExtractor(s, sx)
      Future.sequence(s.completeGames.filterNot(_._1.date.getMonthValue === 11).map { case (g, r) =>
        val ffs = f(g)
        val cxs = c(g, r)
        for {
          fv <- ffs
          xv <- cxs
        } yield {
          (fv.flatMap(_.get("ols-z-diff")), xv.map(_.toInt)) match {
            case (Some(x), Some(y)) => Some(x, y)
            case _ => None
          }
        }
      }).map(_.flatten)
    })).map(_.flatten)
    futureTuples.map(fts=>{
      val (fs,cs) = fts.unzip
      val xs=fs.map(x=>Array(x)).toArray
      val ys = cs.toArray
      BaselineLogisticPredictor(Some(logit(xs, ys)))
    })
  }

  def categoryExtractor(s: Schedule, dx: StatValueDAO): (Game, Result) => Future[Option[Double]] =
    (_,res)=>Future.successful(Some(if (res.homeScore>res.awayScore) 1.0 else 0.0))
}
