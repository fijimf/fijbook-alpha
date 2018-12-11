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

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def loadLatestPredictionModel(key: String): Future[Option[XPredictionModel]] = {
    db.run(repo.xpredictionModels.filter(_.key === key).result.headOption)
  }

  override def loadParametersForModel(modelId: Long): Future[List[XPredictionModelParameter]] = {
    db.run(repo.xpredictionModelParms.filter(_.modelId === modelId).to[List].result)
  }


}
