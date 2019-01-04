package controllers

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.predictors.PredictorContext
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.{Configuration, Logger}
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.Future

class PredictionController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      val dao: ScheduleDAO,
                                      val cfg:Configuration,
                                      cache: AsyncCacheApi,
                                      silhouette: Silhouette[DefaultEnv]
                                    )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  private val predCtx: PredictorContext = PredictorContext(cfg, dao)

  def updatePredictions(key: String, yyyy: Int): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    logger.info(s"Updating predictions for model '$key' and season $yyyy")
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      ps <- predCtx.updatePredictions(key, yyyy)
    } yield {
      Ok(ps.map(_.toString).mkString("\n"))
    }
  }


  def showLatestX(key: String, yyyymmdd: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    val date = controllers.Utils.yyyymmdd(yyyymmdd)
    val value: Future[List[(Game, Option[Result], Option[XPrediction])]] = for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadSchedules()
      pm <- dao.loadLatestPredictionModel(key)
      ps <- dao.findXPredicitions(pm.map(_.id).getOrElse(-1))
    } yield {
      predictResults(ss,ps, date)
    }

    value.map(v=> Ok(v.mkString("\n")))
//    Future.successful)

  }
  def showLatest(key: String, yyyymmdd: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    val date = controllers.Utils.yyyymmdd(yyyymmdd)
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadSchedules()
      pm <- dao.loadLatestPredictionModel(key)
      ps <- dao.findXPredicitions(pm.map(_.id).getOrElse(-1))
    } yield {

      ss.find(_.season.isInSeason(date)).orElse(ss.headOption) match {
        case Some(sch)=>
          implicit val imoplicitSched: Schedule = sch
          Ok(views.html.predictionPage(du,qw,date,key,predictResults(ss,ps, date)))
        case None=> Redirect(routes.ReactMainController.index())
      }
    //  Ok(predictResults(ss,ps, date).mkString("\n"))
    }

  }

  def predictResults(so: List[Schedule], predictions: List[XPrediction], date: LocalDate): List[(Game, Option[Result], Option[XPrediction])] = {
    for {
      sch <- so.find(_.season.isInSeason(date))
    } yield {
      implicit val implicitSchedule: Schedule = sch
      val predMap = predictions.map(p => p.gameId -> p).toMap
      sch.gameResults.filter(_._1.date == date).map(t => (t._1, t._2, predMap.get(t._1.id)))
    }
  }.getOrElse(List.empty[(Game, Option[Result], Option[XPrediction])])
}
