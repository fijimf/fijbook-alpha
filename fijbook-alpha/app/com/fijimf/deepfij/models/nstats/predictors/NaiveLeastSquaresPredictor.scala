package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object NaiveLeastSquaresPredictor extends ModelEngine[String] {
val logger = Logger(this.getClass)
  override val kernel: Option[String] = Some("-")

  def featureExtractor(s: Schedule, ss: StatValueDAO): Game => Future[Option[Map[String, Double]]] = {
    g: Game => {
      for {
        snap <- ss.findXStatsSnapshot(g.seasonId, g.date, "ols", true)
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

  override def predict(s: Schedule, ss: StatValueDAO): Game => Future[Option[XPrediction]] = {
    val f = featureExtractor(s, ss)
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(s)

    g: Game => {
      for {
        features <- f(g)
      } yield {
        features.flatMap(fs => {
          for {
            h <- fs.get("home-raw-ols")
            a <- fs.get("away-raw-ols")
          } yield {
            if (h > a) {
              XPrediction(0L, g.id, 0L, now, hash, Some(g.homeTeamId), None, Some(h - a), Some(h + a))
            } else {
              XPrediction(0L, g.id, 0L, now, hash, Some(g.awayTeamId), None, Some(a - h), Some(h + a))
            }
          }
        })
      }
    }
  }

  override def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[String]] = {
    logger.info("NaiveLeastSquaresPredictor manipulates raw features -- no training necessary")
    Future.successful(this)
  }

}
