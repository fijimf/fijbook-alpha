package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import cats.data.OptionT
import cats.implicits._
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import org.apache.commons.lang3.StringUtils
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

  override def loadLatestPredictionModel(key: String): OptionT[Future, XPredictionModel] = for {
    v <- OptionT(db.run(repo.xpredictionModels.map(_.version).max.result))
    p <- loadPredictionModel(key, v).orElse(savePredictionModel(XPredictionModel(0L, key, 1)))
  } yield p


  def loadPredictionModel(key: String, version: Int): OptionT[Future, XPredictionModel] = OptionT(
    db.run(repo.xpredictionModels.filter(pm => pm.key === key && pm.version === version).result.headOption)
  )

  def loadPredictionModel(key: String): Future[List[XPredictionModel]] = {
    db.run(repo.xpredictionModels.filter(pm => pm.key === key).to[List].result)
  }

  override def savePredictionModel(model: XPredictionModel): OptionT[Future, XPredictionModel] = {
    require(model.version > 0)
    require(StringUtils.isNotBlank(model.key))
    OptionT.liftF(db.run(upsert(model)))
  }

  override def updatePredictions(modelId: Long, schedHash: String, xps: List[XPrediction]): Future[List[XPrediction]] = {
    db.run(
      (for {
        _ <- repo.xpredictions.filter(x => x.modelId === modelId && x.schedMD5Hash === schedHash).delete
        _ <- repo.xpredictions ++= xps.map(_.copy(modelId = modelId, schedMD5Hash = schedHash))
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

  override def findXPredicitions(modelId: Long): Future[List[XPrediction]] = {
    db.run(repo.xpredictions.filter(_.modelId === modelId).to[List].result)
  }

}
