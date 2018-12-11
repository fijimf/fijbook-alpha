package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction, XPredictionModelParameter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import smile.classification._
object NaiveLeastSquaresPredictor extends ModelEngine {


  override def featureExtractor(s: Schedule, ss:StatValueDAO): Game => Future[Option[Map[String, Double]]] = {
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

  override def predictor(s:Schedule, ss:StatValueDAO): (Game, Long) => Future[Option[XPrediction]]={
    val f = featureExtractor(s,ss)
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(s)

    (g:Game, id:Long)=> {
       for {
         features<-f(g)
       } yield {
         features.flatMap(fs=>{
           for {
             h <- fs.get("home-raw-ols")
             a <- fs.get("away-raw-ols")
           } yield {
             if (h>a) {
               XPrediction(0L,g.id, id,now,hash,Some(g.homeTeamId),None,Some(h-a),Some(h+a))
             } else {
               XPrediction(0L,g.id, id,now,hash,Some(g.awayTeamId),None,Some(a-h),Some(h+a))
             }
           }
         })
       }
    }
  }

  override def train(ss: List[Schedule], sx: StatValueDAO): List[XPredictionModelParameter] = {

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
      val logisticRegression = logit(xs,ys)
      logisticRegression.predict()

    })



  }

  override def categoryExtractor(s: Schedule, dx: StatValueDAO): (Game, Result) => Future[Option[Double]] =
    (_,res)=>Future.successful(Some(if (res.homeScore>res.awayScore) 1.0 else 0.0))
}
