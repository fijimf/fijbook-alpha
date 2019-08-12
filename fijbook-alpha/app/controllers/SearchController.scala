package controllers

import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.{Conference, Quote, RssFeed, RssItem, Team}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext
import controllers.Utils._
class SearchController @Inject()(
                                     val controllerComponents: ControllerComponents,
                                     val dao: ScheduleDAO,
                                     val cache: AsyncCacheApi,
                                     val userService: UserService,
                                     val silhouette: Silhouette[DefaultEnv],
                                     val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  def search(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    rs.request.queryString.get("q").flatMap(_.headOption) match {
      case None => for {
        du <- loadDisplayUser(rs)
        qw <- getQuoteWrapper(du)
      } yield {
        Ok(views.html.searchResults(du, qw, List.empty[Team], List.empty[Conference], List.empty[Quote], List.empty[(RssItem, RssFeed)]))
      }
      case Some(str) =>
        val k = str.trim()
        for {
          du <- loadDisplayUser(rs)
          qw <- getQuoteWrapper(du)
          ts <- dao.findTeamsLike(k)
          cs <- dao.findConferencesLike(k)
          qs <- dao.findQuotesLike(k)
          ns <- dao.findRssItemsLike(k)
        } yield {
          Ok(views.html.searchResults(du, qw, ts.sortBy(_.name), cs.sortBy(_.name), qs, ns.sortBy(-_._1.publishTime.toMillis)))
        }
    }
  }
}




