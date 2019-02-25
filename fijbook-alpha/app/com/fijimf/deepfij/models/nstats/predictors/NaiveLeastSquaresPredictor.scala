package com.fijimf.deepfij.models.nstats.predictors

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NaiveLeastSquaresPredictor extends ModelEngine[String] {
val logger = Logger(this.getClass)
  override def predict(s: Schedule, ss: StatValueDAO): List[Game] => Future[List[XPrediction]] = {
    val f = NaiveLeastSquaresFeatureExtractor(s, ss)
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(s)

    gs: List[Game] => {
      for {
        features <- f(gs)
      } yield features.zip(gs).flatMap { case (feat, g) =>
        for {
          h <- feat.get("home-raw-ols")
          a <- feat.get("away-raw-ols")
        } yield {
          if (h > a) {
            XPrediction(0L, g.id, 0L, now, hash, Some(g.homeTeamId), None, Some(h - a), None)
          } else {
            XPrediction(0L, g.id, 0L, now, hash, Some(g.awayTeamId), None, Some(a - h), None)
          }
        }
      }
    }
  }

  override def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[String]] = {
    logger.info("NaiveLeastSquaresPredictor manipulates raw features -- no training necessary")
    Future.successful(this)
  }

}
