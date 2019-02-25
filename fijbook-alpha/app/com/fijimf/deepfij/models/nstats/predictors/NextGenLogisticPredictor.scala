package com.fijimf.deepfij.models.nstats.predictors

import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}
import play.api.Logger
import smile.classification._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class NextGenLogisticPredictor(kernel: Option[LogisticRegression], trainedAt:LocalDateTime) extends ModelEngine[LogisticRegression] {
val logger=Logger(this.getClass)


  override def train(ss: List[Schedule], sx: StatValueDAO): Future[ModelEngine[LogisticRegression]] = {
    val futureTuples: Future[List[(Double, Int)]] = Future.sequence(ss.map(s => loadFeaturesAndCategories(s, sx))).map(_.flatten)

    futureTuples.map(fts=>{
      logger.info(s"Training set has ${fts.size} a elements")
      val (fs,cs) = fts.unzip
      val xs=fs.map(x=>Array(x)).toArray
      val ys = cs.toArray
      NextGenLogisticPredictor(Some(logit(xs, ys)),LocalDateTime.now())
    })
  }

  private def loadFeaturesAndCategories(s: Schedule, sx: StatValueDAO): Future[List[(Double, Int)]] = {

    val f: BaseLogisticFeatureExtractor = BaseLogisticFeatureExtractor(s, sx)
    val c: CategoryExtractor = SpreadCategoryExtractor(SpreadTransformers.cap(35))
    val cPrime: CategoryExtractor = SpreadCategoryExtractor(SpreadTransformers.cappedNoisy(35,0.025))
    val games = s.completeGames.filterNot(_._1.date.getMonthValue === 11)
    logger.info(s"For schedule ${s.season.year} found ${games.size} games")
    for {
      features <- f(games.map(_._1))
      cats <- c(games)
      catsPrime <-cPrime(games)
    } yield {
      val obs = features.zip(cats).flatMap {
        case (featureMap: Map[String, Double], cat: Option[Double]) =>
          (featureMap.get("ols-z-diff"), cat.map(c=>math.round(c).toInt+35)) match {
            case (Some(x), Some(y)) => Some(x, y)
            case _ => None
          }
      }
      val obsPrimes = features.zip(catsPrime).flatMap {
        case (featureMap: Map[String, Double], cat: Option[Double]) =>
          (featureMap.get("ols-z-diff"), cat.map(c=>math.round(c).toInt+35)) match {
            case (Some(x), Some(y)) => Some(x, y)
            case _ => None
          }
      }
      val o2 = obs++obsPrimes
      logger.info(s"For schedule ${s.season.year} found ${o2.size} observations")
      val catValues=Set(o2.map(_._2))
      logger.info(s"${catValues.size} categories from ${catValues.min} to ${catValues.max} ")
      o2
    }
  }


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
                val pp = Array.fill[Double](71)(0.0)
                val p: Int = k.predict(Array(x), pp)
                val spread=pp.zipWithIndex.map{case (probability: Double, spreadOff: Int) => probability * (spreadOff-35)}.sum
                val hp1=pp.drop(35).sum
                val ap1=pp.take(36).sum
                val hp = hp1+(1.0-(hp1+ap1))/2
                val ap = ap1+(1.0-(hp1+ap1))/2

                logger.info(s"For game (${g.id} | ${g.date}), feature $x => probability $p")
                if (hp > ap ) {
                  XPrediction(0L, g.id, 0L, now, "", Some(g.homeTeamId), Some(hp), Some(math.max(spread, 0.0)), None)
                } else {
                  XPrediction(0L, g.id, 0L, now, "", Some(g.awayTeamId), Some(ap), Some(math.max(-spread,0.0)), None)
                }
              })
            }
          }.flatten
      case _ => gs: List[Game] => Future.successful(List.empty[XPrediction])
    }
  }
}
