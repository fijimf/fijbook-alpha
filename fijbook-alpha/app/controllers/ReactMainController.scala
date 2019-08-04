package controllers

import java.time.{LocalDate, LocalDateTime}

import cats.effect.IO
import com.fijimf.deepfij.model.schedule.{Season, Team}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{FrontPageData, Game, Quote, Result, Schedule}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import modules.TransactorCtx
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import doobie.implicits._
import doobie.util.ExecutionContexts
import utils.PlayIO

import scala.concurrent.{ExecutionContext, Future}

class ReactMainController @Inject()(
                                     val controllerComponents: ControllerComponents,
                                     val dao: ScheduleDAO,
                                     val cache: AsyncCacheApi,
                                     val userService: UserService,
                                     val silhouette: Silhouette[DefaultEnv],
                                     val s3BlockController: S3BlockController,
                                     val transactorCtx: TransactorCtx)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with PlayIO with UserEnricher with QuoteEnricher with I18nSupport {

  import controllers.Utils._

  val NO_GAMES = List.empty[(Game, Option[Result])]
  val xa = transactorCtx.xa

  def supercool(): Action[AnyContent] = Action.io(ExecutionContexts.synchronous) { request =>
    Season.Dao(xa).select.query[Season].to[List].transact(xa).map(_.mkString(",")).map(Ok(_))
  }

  def index(): Action[AnyContent] = silhouette.UserAwareAction.io { implicit rs =>
    IO.fromFuture(IO {
      frontPageForDate(LocalDate.now())
    })

  }

  def dateIndex(d: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    frontPageForDate(yyyymmdd(d))
  }

  private def frontPageForDate(today: LocalDate)(implicit rs: UserAwareRequest[DefaultEnv, AnyContent]) = {
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      schedules <- loadSchedules
      quotes <- dao.getWeeklyVoteCounts(today)
      items <- dao.findRssItemsByDate(today.atStartOfDay(), 7)
    } yield {
      val (todaysGames, yesterdaysGames,osched) = schedules.find(_.season.isInSeason(today)) match {
        case Some(sched) =>
          (gamesForDate(today, sched), gamesForDate(today.minusDays(1), sched), Some(sched))
        case None => (NO_GAMES, NO_GAMES, Option.empty[Schedule])
      }

      val (q, n, d)= quotes match {
        case Nil => (Quote(-1L, "", None, None, None), 0, LocalDateTime.now())
        case list =>
          val topVotes = list.filter(_._2 == list.maxBy(_._2)._2)
          topVotes.maxBy(_._3.toMillis)
      }
      val f = FrontPageData(today, osched, todaysGames, yesterdaysGames, q,n,d, items.sortBy(-_._1.publishTime.toMillis).take(12))


      Ok(views.html.frontPage(du, qw, f))
    }
  }

  private def gamesForDate(today: LocalDate, sched: Schedule): List[(Game, Option[Result])] = {
    sched.gameResults.filter(_._1.date == today).groupBy(_._1.datetime).toList.sortBy(_._1.toMillis).map(_._2).flatten
  }

  private def loadSchedules: Future[List[Schedule]] = {
    cache.getOrElseUpdate[List[Schedule]]("deepfij.schedules", hotCacheDuration) {
      dao.loadSchedules()
    }
  }
}




