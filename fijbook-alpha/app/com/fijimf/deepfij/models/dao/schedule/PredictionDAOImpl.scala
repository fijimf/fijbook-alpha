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

  private def upsert(model: XPredictionModel) = {
    (repo.xpredictionModels returning repo.xpredictionModels.map(_.id)).insertOrUpdate(model).flatMap {
      case Some(id) => repo.xpredictionModels.filter(_.id === id).result.head
      case None => DBIO.successful(model)
    }
  }



}
