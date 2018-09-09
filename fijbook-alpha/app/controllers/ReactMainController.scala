package controllers

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{FrontPageData, Game, Quote, Result}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

class ReactMainController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import controllers.Utils._

  val NO_GAMES = List.empty[(Game, Option[Result])]

  def index(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    frontPageForDate(LocalDate.now())
  }

  def dateIndex(d: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    frontPageForDate(yyyymmdd(d))
  }

  private def frontPageForDate(today: LocalDate)(implicit rs: UserAwareRequest[DefaultEnv, AnyContent]) = {
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      schedules <- dao.loadSchedules()
      quotes <- dao.getWeeklyVoteCounts(today)
      items <- dao.findRssItemsByDate(today.atStartOfDay(), 7)
    } yield {
      val (todaysGames, yesterdaysGames) = schedules.find(_.season.isInSeason(today)) match {
        case Some(sched) =>
          (sched.gameResults.filter(_._1.date == today), sched.gameResults.filter(_._1.date == today.minusDays(1)))
        case None => (NO_GAMES, NO_GAMES)
      }

      val (q, n, d)= quotes match {
        case Nil => (Quote(-1L, "", None, None, None), 0, LocalDateTime.now())
        case list =>
          val topVotes = list.filter(_._2 == list.maxBy(_._2)._2)
          topVotes.maxBy(_._3.toMillis)
      }
      val f = FrontPageData(today, todaysGames, yesterdaysGames, q,n,d, items.sortBy(- _._1.publishTime.toMillis).take(12))


      Ok(views.html.frontPage(du, qw, f))
    }
  }
}




