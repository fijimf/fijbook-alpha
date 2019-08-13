package controllers

import java.time.LocalDateTime

import com.fijimf.deepfij.models.FavoriteLink
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.statistics.services.ComputedStatisticService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc._

class UserController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dao: ScheduleDAO,
                                val statWriterService: ComputedStatisticService,
                                cache: AsyncCacheApi,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def saveFavoriteLink(title:String, url:String):  Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    for {
      _ <- dao.saveFavoriteLink(FavoriteLink(0L, request.identity.userID.toString, title, url, 0, LocalDateTime.now()))
    } yield {
      request.headers.get("referer") match {
        case Some(u)=> Redirect(Call("GET", u))
        case None => Redirect(routes.ReactMainController.index())
      }
    }
  }

  def deleteFavoriteLink(url:String):  Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    for {
      _ <- dao.deleteFavoriteLinkForUser(request.identity.userID.toString,  url)
    } yield {
      request.headers.get("referer") match {
        case Some(u)=> Redirect(Call("GET", u))
        case None => Redirect(routes.ReactMainController.index())
      }
    }
  }
}