package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}

class StatController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dao: ScheduleDAO,
                                cache: AsyncCacheApi,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  val logger = Logger(getClass)

  def showStat(key: String) =  silhouette.SecuredAction.async { implicit rs => TODO(rs) }

  def showStat(key: String, yyyymmdd:String) =  silhouette.SecuredAction.async { implicit rs => TODO(rs) }

  def stats() =  silhouette.SecuredAction.async { implicit rs => TODO(rs) }

  def showStatSnapshot(key: String, yyyymmdd: String) =  silhouette.SecuredAction.async { implicit rs => TODO(rs) }
}