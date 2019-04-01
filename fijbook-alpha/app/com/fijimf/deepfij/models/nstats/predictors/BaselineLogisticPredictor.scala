package com.fijimf.deepfij.models.nstats.predictors

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.LocalDate
import java.util.Base64

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Schedule, XPrediction}
import play.api.Logger
import smile.classification._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class BaselineLogisticPredictor(modelId:Long, version:Int, kernel: Option[String] = None) extends Predictor {
  val logger = Logger(this.getClass)

  override def key: String = "win-based-logistic"

  val logisticRegression: Option[LogisticRegression] = kernel.flatMap(s => deserializeKernel(s))

  def featureExtractor(schedule: Schedule, statDao: StatValueDAO): FeatureExtractor = BaseLogisticFeatureExtractor(statDao)

  def categoryExtractor: CategoryExtractor = WinLossCategoryExtractor()

  def loadFeaturesAndCategories(schedule: Schedule, statDao: StatValueDAO): Future[List[(Array[Double], Int)]] = {
    val games = schedule.completeGames.filterNot(_._1.date.getMonthValue === 11)
    logger.info(s"For schedule ${schedule.season.year} found ${games.size} games")
    for {
      features <- featureExtractor(schedule, statDao)(games.map(_._1))
      categories <- categoryExtractor(games)
    } yield {
      val observations = features.zip(categories).flatMap {
        case (featureMap: (Long,Map[String, Double]), cat: (Long,Option[Double])) =>
          (featureMap._2.get("ols.zscore.diff"), cat._2.map(_.toInt)) match {
            case (Some(x), Some(y)) => Some(Array(x), y)
            case _ => None
          }
      }
      logger.info(s"For schedule ${schedule.season.year} found ${observations.size} observations")
      observations
    }
  }

  def train(ss: List[Schedule], sx: StatValueDAO): Future[Option[String]] = {
    val observations: Future[List[(Array[Double], Int)]] = Future.sequence(ss.map(s => loadFeaturesAndCategories(s, sx))).map(_.flatten)

    observations.map(obs => {
      logger.info(s"Training set has ${obs.size} a elements")
      val (featureVectors, categories) = obs.unzip
      val xs = featureVectors.toArray
      val ys = categories.toArray
      val logisticRegression: LogisticRegression = logit(xs, ys)
      serializeKernel(logisticRegression)
    })
  }

  def serializeKernel(lr: LogisticRegression): Option[String] = {
    Try {
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(lr)
      oos.close()
      Base64.getEncoder.encodeToString(baos.toByteArray)
    }.toOption
  }

  def deserializeKernel(s: String): Option[LogisticRegression] = {
    Try {
      val bais = new ByteArrayInputStream(Base64.getDecoder.decode(s))
      val ois = new ObjectInputStream(bais)
      ois.readObject().asInstanceOf[LogisticRegression]
    }.toOption
  }

  def predict(schedule: Schedule, statDao: StatValueDAO): Future[List[XPrediction]] = {
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(schedule)
    val gs = schedule.incompleteGames
    logisticRegression match {
      case None => Future.successful(List.empty[XPrediction])
      case Some(lr) =>
        for {
          features <- featureExtractor(schedule, statDao)(gs)
        } yield {
          gs.zip(features).flatMap { case (g, feat) =>
            feat._2.get("ols.zscore.diff").map(x => {
              val pp = Array[Double](Double.NaN, Double.NaN)
              val p = lr.predict(Array(x), pp)
              logger.info(s"For game (${g.id} | ${g.date}), feature $x => probability $p")
              if (p === 1) {
                XPrediction(0L, g.id, modelId, now, hash, Some(g.homeTeamId), Some(pp(1)), None, None)
              } else {
                XPrediction(0L, g.id, modelId, now, hash, Some(g.awayTeamId), Some(pp(0)), None, None)
              }
            })
          }
        }
    }
  }
}
