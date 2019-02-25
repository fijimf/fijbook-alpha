package com.fijimf.deepfij.models.nstats.predictors

import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction}
import play.api.Logger
import smile.classification._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BaselineLogisticPredictor(kernel: Option[LogisticRegression], trainedAt:LocalDateTime) extends ModelEngine[LogisticRegression] {
val logger=Logger(this.getClass)

  override def predict(s: Schedule, ss: StatValueDAO): List[Game] => Future[List[XPrediction]] = {
    val f = BaseLogisticFeatureExtractor(s, ss)
    val now = LocalDate.now()
    kernel match {
      case Some(k) =>
        gs: List[Game] =>
          logger.info(s"Running predictions for ${gs.size} games.")
          logger.info(s"Start loading features.")
          for {
            features <- f(gs)
          } yield {
            logger.info(s"Done loading features.")
            features.zip(gs).map { case (feat, g) =>
              feat.get("ols-z-diff").map(x => {
                val pp = Array[Double](Double.NaN, Double.NaN)
                val p = k.predict(Array(x), pp)
                logger.info(s"For game (${g.id} | ${g.date}), feature $x => probability $p")
                if (p === 1) {
                  XPrediction(0L, g.id, 0L, now, "", Some(g.homeTeamId), Some(pp(1)), None, None)
                } else {
                  XPrediction(0L, g.id, 0L, now, "", Some(g.awayTeamId), Some(pp(0)), None, None)
                }
              })
            }
          }.flatten
      case _ => gs: List[Game] => Future.successful(List.empty[XPrediction])
    }
  }

  override def train(ss: List[Schedule], sx: StatValueDAO): Future[ModelEngine[LogisticRegression]] = {
    val futureTuples: Future[List[(Double, Int)]] = Future.sequence(ss.map(s => loadFeaturesAndCategories(s, sx))).map(_.flatten)

    futureTuples.map(fts=>{
      logger.info(s"Training set has ${fts.size} a elements")
      val (fs,cs) = fts.unzip
      val xs=fs.map(x=>Array(x)).toArray
      val ys = cs.toArray
      BaselineLogisticPredictor(Some(logit(xs, ys)), LocalDateTime.now())
    })
  }

  private def loadFeaturesAndCategories(s: Schedule, sx: StatValueDAO): Future[List[(Double, Int)]] = {

    val f: BaseLogisticFeatureExtractor = BaseLogisticFeatureExtractor(s, sx)
    val c: CategoryExtractor = WinLossCategoryExtractor()
    val games = s.completeGames.filterNot(_._1.date.getMonthValue === 11)
    logger.info(s"For schedule ${s.season.year} found ${games.size} games")
    for {
      features <- f(games.map(_._1))
      categories <- c(games)
    } yield {
      val observations = features.zip(categories).flatMap {
        case (featureMap: Map[String, Double], cat: Option[Double]) =>
          (featureMap.get("ols-z-diff"), cat.map(_.toInt)) match {
            case (Some(x), Some(y)) => Some(x, y)
            case _ => None
          }
      }
      logger.info(s"For schedule ${s.season.year} found ${observations.size} observations")
      observations
    }
  }

  def categoryExtractor(s: Schedule, dx: StatValueDAO): (Game, Result) => Future[Option[Double]] =
    (_,res)=>Future.successful(Some(if (res.homeScore>res.awayScore) 1.0 else 0.0))
}
