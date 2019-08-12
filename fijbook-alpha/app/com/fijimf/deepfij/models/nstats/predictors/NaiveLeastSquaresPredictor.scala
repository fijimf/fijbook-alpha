package com.fijimf.deepfij.models.nstats.predictors

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Schedule, XPrediction}
import com.fijimf.deepfij.schedule.services.ScheduleSerializer
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class NaiveLeastSquaresPredictor(modelId:Long, version:Int) extends Predictor {

  override def key: String = "naive-least-squares"

  val logger = Logger(this.getClass)

  def featureExtractor(schedule: Schedule, statDao: StatValueDAO): FeatureExtractor = NaiveLeastSquaresFeatureExtractor(statDao)

  def categoryExtractor: CategoryExtractor = SpreadCategoryExtractor(r=>Some(r),x=>Some(x))

  def loadFeaturesAndCategories(schedule: Schedule, statDao: StatValueDAO): Future[List[(Array[Double], Int)]] = Future.successful(List.empty[(Array[Double], Int)])

  def train(ss: List[Schedule], sx: StatValueDAO): Future[Option[String]] = Future.successful(Some(LocalDateTime.now.toString))

  def predict(schedule: Schedule, statDao: StatValueDAO): Future[List[XPrediction]] = {
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(schedule)
    val gs = schedule.incompleteGames

    for {
      features <- featureExtractor(schedule,statDao)(gs)
    } yield {
      gs.zip(features).flatMap { case (g, feat) =>
        for {
          s <- feat._2.get("ols.value.diff") if s != 0.0
        } yield {
          if (s > 0) {
            XPrediction(0L, g.id, modelId, now, hash, Some(g.homeTeamId), None, Some(s), None)
          } else {
            XPrediction(0L, g.id, modelId, now, hash, Some(g.awayTeamId), None, Some(-s), None)
          }
        }
      }
    }
  }
}
