package controllers

import java.time.LocalDate

import cats.data.OptionT
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.predictors.{PredictionResult, Predictor, PredictorContext}
import com.fijimf.deepfij.models.react.{DisplayUser, QuoteWrapper}
import com.fijimf.deepfij.models.{Schedule, XPrediction}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.silhouette.utils.DefaultEnv
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Logger}

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

  private val predCtx: PredictorContext = PredictorContext( dao)


  def managePredictions(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val ts = Future.sequence(Predictor.keys.map(key => {
      dao.loadPredictionModels(key).map(lst => key -> lst)
    }))

    val aps = dao.findAllPredictions()

    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      predictorLists <- ts
      predictionMap  <- aps
    } yield {
      Ok(views.html.admin.managePredictions(du, qw, predictorLists, predictionMap)) //<--TODO forward to display the predictions
    }
  }

  def updateLatestPredictions(key: String, yyyy: Int): Action[AnyContent] = TODO //silhouette.UserAwareAction.async { implicit rs =>
//    logger.info(s"Updating predictions for latest trained version model '$key' and season $yyyy")
//    for {
//      du <- loadDisplayUser(rs)
//      qw <- getQuoteWrapper(du)
//      ps <- predCtx.updatePredictions(key, yyyy)
//    } yield {
//      Ok(ps.map(_.toString).mkString("\n")) //<--TODO forward to display the predictions
//    }
//  }

  def updatePredictions(key:String, version:Int): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    logger.info(s"Updating predictions for version $version of model '$key' ")
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      ps <- Predictor.updatePredictions(key, version, dao).value
    } yield {
      Redirect(routes.PredictionController.managePredictions()).flashing("info" -> s"Updated ${ps.getOrElse(0)} predictions. ")
    }
  }

//  def showLatest(key: String, yyyymmdd: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
//    val date = controllers.Utils.yyyymmdd(yyyymmdd)
//    for {
//      du <- loadDisplayUser(rs)
//      qw <- getQuoteWrapper(du)
//      ss <- dao.loadSchedules()
//      pm <- dao.loadLatestPredictionModel(key)
//      ps <- dao.findXPredicitions(pm.id)
//    } yield {
//      ss.find(_.season.isInSeason(date)).orElse(ss.headOption) match {
//        case Some(sch) =>
//          implicit val impSched: Schedule = sch
//          val trainedAt = Some(LocalDateTime.ofInstant(Predictor.modelTrainedAt(cfg, pm.key, pm.version).toInstant, TimeZone.getTimeZone("America/New_York").toZoneId))
//          Ok(views.html.predictionPage(du, qw, key, date, pm, trainedAt, predictResults(ss, ps, date)))
//        case None => Redirect(routes.ReactMainController.index())
//      }
//    }
//
//  }

  def predictResults(so: List[Schedule], predictions: List[XPrediction], date: LocalDate): List[PredictionResult] = {
    for {
      sch <- so.find(_.season.isInSeason(date))
    } yield {
      val predMap = predictions.map(p => p.gameId -> p).toMap
      sch.gameResults.filter(_._1.date == date).map(t => PredictionResult(t._1, t._2, predMap.get(t._1.id)))
    }
  }.getOrElse(List.empty[PredictionResult])

  def trainModel(key: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      oxpm <- Predictor.train(key, dao).value
    } yield {
      oxpm match {
        case Some(predictor) => Redirect(routes.PredictionController.managePredictions()).flashing("info" -> s"Trained new '$key'. ")
        case None => Redirect(routes.PredictionController.managePredictions()).flashing("error" -> s"Failed to train new '$key'. ")
      }
    }
  }

  def compareModels(key1: String, version1: Int, key2: String, version2: Int, yyyyy: Int) = play.mvc.Results.TODO

  def showVersion(key: String, version:Int, yyyymmdd: String) = play.mvc.Results.TODO

  def showAll(): Action[AnyContent] = TODO //silhouette.UserAwareAction.async { implicit rs =>

  def showVersions(key: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    val date = LocalDate.now()
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadSchedules()
      page: Option[Result] <- buildPage(key,date,  du, qw, ss)
    } yield {
      page match {
        case Some(p) => p
        case None=>Redirect(routes.ReactMainController.index())
      }
    }
  }



  private def buildPage(key: String, date: LocalDate, du: DisplayUser, qw: QuoteWrapper, ss: List[Schedule])(implicit rs:Request[ AnyContent]) = {
    import cats.implicits.catsStdInstancesForFuture
    ss.find(_.season.isInSeason(date)).orElse(ss.headOption) match {
      case Some(sch) =>
        (for {
          model <- dao.loadLatestPredictionModel(key)
          predictions <- dao.findXPredictions(model.id)
        } yield {
          implicit val impSched: Schedule = sch
          val trainedAt = if (model.isTrained) Some(model.createdAt) else None
          Ok(views.html.predictionPage(du, qw, key, date, model, trainedAt, predictResults(ss, predictions, date)))
        }).value

    }
  }
}
