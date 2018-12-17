package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait PredictionDAOImpl extends PredictionDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def loadLatestPredictionModel(key: String): Future[Option[XPredictionModel]] = {
    db.run(repo.xpredictionModels.filter(_.key === key).result.headOption)
  }

  override def saveXPredictionModel(model: XPredictionModel): Future[XPredictionModel] = db.run(upsert(model))

  override def updateXPredictions(modelId: Long, schedHash: String, xps: List[XPrediction]): Future[List[XPrediction]] = {
    db.run(
      (for {
        _ <- repo.xpredictions.filter(x => x.modelId === modelId && x.schedMD5Hash === schedHash).delete
        _ <- repo.xpredictions ++= xps
        ins <- repo.xpredictions.filter(x => x.modelId === modelId && x.schedMD5Hash === schedHash).result
      } yield {
        ins
      }.to[List]).transactionally
    )
  }


  private def upsert(model: XPredictionModel) = {
    (repo.xpredictionModels returning repo.xpredictionModels.map(_.id)).insertOrUpdate(model).flatMap {
      case Some(id) => repo.xpredictionModels.filter(_.id === id).result.head
      case None => DBIO.successful(model)
    }
  }

  override def findXPredicitions(modelId:Long):Future[List[XPrediction]] ={
    db.run(repo.xpredictions.filter(_.modelId===modelId).to[List].result)
  }

}
