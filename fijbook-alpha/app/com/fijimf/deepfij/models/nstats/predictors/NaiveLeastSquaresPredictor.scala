package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction, XPredictionModelParameter}

import scala.concurrent.Future
import cats.implicits._

object NaiveLeastSquaresPredictor extends ModelEngine {


  override def featureExtractor(s: Schedule, ss:StatValueDAO): Game => Future[Option[Map[String, Double]]] = {
    g:Game=> {
      for {
        snap <- ss.findXStatsSnapshot(g.seasonId, g.date, "ols")
      } yield {
        for {
          h <- snap.find(_.teamId === g.homeTeamId).flatMap(_.value)
          a <- snap.find(_.teamId === g.awayTeamId).flatMap(_.value)
        } yield {
          Map(
            "home-raw-ols" -> h,
            "away-raw-ols" -> a
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

  override def train(s: List[Schedule]): List[XPredictionModelParameter] = List.empty[XPredictionModelParameter]

  override def categoryExtractor(s: Schedule, dx: StatValueDAO): (Game, Result) => Future[Option[Double]] = throw new NotImplementedError()
}
