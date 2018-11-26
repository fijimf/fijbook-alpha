package controllers

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.cibo.evilplot.geometry.Extent
import com.cibo.evilplot.numeric.Point
import com.cibo.evilplot.plot.renderers.BarRenderer
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.Analysis
import com.fijimf.deepfij.models.react.DisplayLink
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.imageio.ImageIO
import org.apache.commons.io.IOUtils
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.Future
import scala.util.Random

class StatController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dao: ScheduleDAO,
                                cache: AsyncCacheApi,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)


  def team(key: String, season: Option[Int]): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    (for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap(tup => {
      dao.loadSchedules().flatMap(ss => {
        if (ss.isEmpty) {
          Future.successful(Redirect(routes.ReactMainController.index()).flashing("info" -> "No schedules are loaded."))
        } else {
          val year = season match {
            case Some(y) => y
            case None => ss.map(_.season.year).max
          }
          val seasonKeys = ss.map(_.season.year).sorted.reverse
          ss.find(_.season.year === year) match {
            case Some(sch) =>
              val t: Team = sch.keyTeam(key)
              val stats = loadTeamStats(t, sch)
              stats.map(lstat => {
                Ok(views.html.data.team(tup._1, tup._2, t, sch, lstat, seasonKeys, DisplayLink(t.name, routes.TeamController.team(t.key, None).url, "#")))
              })
            case None => Future.successful(Redirect(routes.ReactMainController.index()).flashing("info" -> s"No schedule found for year $year"))
          }
        }
      })
    })
  }

  def teamGames(key: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    dao.loadSchedules().map(f => {
      f.flatMap(s => {
        s.teams.find(_.key === key) match {
          case Some(team) => getTeamGames(s, team)
          case None => List.empty[JsObject]
        }
      })
    }).map(l => JsArray.apply(l)).map(Ok(_))
  }


  private def getTeamGames(s: Schedule, team: Team): List[JsObject] = {
    val year = s.season.year
    s.gameResults.flatMap {
      case (g, Some(r)) if g.homeTeamId === team.id =>
        val date = g.date.format(DateTimeFormatter.ISO_DATE)
        val wonLost = if (r.isHomeWinner) "W" else "L"
        val opponent = s.teamsMap(g.awayTeamId)
        val location = g.location.getOrElse("")
        val longDate = g.datetime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        val time = g.datetime.format(DateTimeFormatter.ofPattern("hh:mm "))
        val han = if (g.isNeutralSite) "neutral-game" else "home-game"
        Some(
          Json.obj(
            "season" -> Json.toJson(year),
            "date" -> Json.toJson(date),
            "longdate" -> Json.toJson(longDate),
            "time" -> Json.toJson(time),
            "location" -> Json.toJson(location),
            "oppName" -> Json.toJson(opponent.name),
            "oppKey" -> Json.toJson(opponent.key),
            "oppLogo" -> Json.toJson(opponent.logoSmUrl.getOrElse("")),
            "wonLost" -> Json.toJson(wonLost),
            "atVs" -> Json.toJson("v"),
            "score" -> Json.toJson(r.homeScore),
            "oppScore" -> Json.toJson(r.awayScore),
            "homeAwayClass" -> Json.toJson(han)
          )
        )
      case (g, Some(r)) if g.awayTeamId === team.id =>
        val date = g.date.format(DateTimeFormatter.ISO_DATE)
        val wonLost = if (r.isAwayWinner) "W" else "L"
        val opponent = s.teamsMap(g.homeTeamId)
        val location = g.location.getOrElse("")
        val longDate = g.datetime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        val time = g.datetime.format(DateTimeFormatter.ofPattern("h:mm a"))
        val han = if (g.isNeutralSite) "neutral-game" else "away-game"
        Some(
          Json.obj(
            "season" -> Json.toJson(year),
            "date" -> Json.toJson(date),
            "longDate" -> Json.toJson(longDate),
            "time" -> Json.toJson(time),
            "location" -> Json.toJson(location),
            "oppName" -> Json.toJson(opponent.name),
            "oppKey" -> Json.toJson(opponent.key),
            "oppLogo" -> Json.toJson(opponent.logoSmUrl.getOrElse("")),
            "wonLost" -> Json.toJson(wonLost),
            "atVs" -> Json.toJson("@"),
            "score" -> Json.toJson(r.awayScore),
            "oppScore" -> Json.toJson(r.homeScore),
            "homeAwayClass" -> Json.toJson(han)
          )
        )
      case (_, _) => None
    }
  }

  def loadTeamStats(t: Team, sch: Schedule): Future[List[ModelTeamContext]] = {
    Future.sequence(Analysis.models.map { case (display, ananlysis) => {
      dao.findXStatsLatest(sch.season.id, t.id, ananlysis.key).map(_.map(xs => (display, ananlysis, xs)))
    }
    }).map(_.flatten.toList.map {
      case (d, a, x) =>
        ModelTeamContext(x.seasonId, x.date, d, a.key, x.value, x.rank, x.mean, x.stdDev, a.fmtString)
    })
  }

  def teams(q: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadLatestSchedule()
    } yield {
      ss match {
        case Some(sch) =>
          val matches = sch.teams.filter(t => {
            val qryStr = q.trim.toUpperCase
            t.name.toUpperCase.contains(qryStr) || t.longName.toUpperCase.contains(qryStr) || t.nickname.toUpperCase.contains(qryStr)
          })
          matches match {
            case Nil =>
              val columns = sch.teams.sortBy(_.name).grouped((sch.teams.size + 3) / 4).toList
              Ok(views.html.data.teams(du, qw, columns)).flashing("info" -> ("No matching teams for query string '" + q + "'"))
            case t :: Nil => Redirect(routes.TeamController.team(t.key, None))
            case lst =>
              val columns = lst.sortBy(_.name).grouped((lst.size + 3) / 4).toList
              Ok(views.html.data.teams(du, qw, columns))
          }
        case None => Redirect(routes.ReactMainController.index()).flashing("info" -> "No current schedule loaded")
      }
    }
  }

  def conference(key: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadLatestSchedule()
    } yield {
      ss match {
        case Some(sch) =>
          val c = sch.conferenceKeyMap(key)
          Ok(views.html.data.conference(du, qw, c, sch.conferenceStandings(c), sch.interConfRecord(c), sch.nonConferenceSchedule(c), sch.conferenceSchedule(c), sch))
        case None => Redirect(routes.ReactMainController.index()).flashing("info" -> "No current schedule loaded")
      }

    }

  }

  def conferences(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      ss <- dao.loadLatestSchedule()
    } yield {
      ss match {
        case Some(sch) =>
          val cmap = sch.conferences.map(c => c -> sch.conferenceStandings(c)).sortBy(_._1.name)
          Ok(views.html.data.conferences(du, qw, cmap))
        case None => Redirect(routes.ReactMainController.index()).flashing("info" -> "No current schedule loaded")
      }

    }
  }

  def timeSeries(seasonId: Long, key: String, teamId: Long): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    import com.cibo.evilplot.colors._
    import com.cibo.evilplot.plot._
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      lst <- dao.findXStatsTimeSeries(seasonId, teamId, key)
    } yield {
      Ok(lst.size.toString)
    }
  }



  def histogram(seasonId: Long, key: String, yyyymmdd: Int, width:Double=1000.0, height:Double=500.0): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    import com.cibo.evilplot.colors._
    import com.cibo.evilplot.plot._
    import com.cibo.evilplot.plot.aesthetics.DefaultTheme._
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      lst <- dao.findXStatsSnapshot(seasonId, LocalDate.parse(yyyymmdd.toString,DateTimeFormatter.ofPattern("yyyyMMdd")), key)
    } yield {
      val file = File.createTempFile("dfff",".png")
      val plot: Plot = Histogram(lst.flatMap(_.value))
      plot
        .xLabel(Analysis.models.find(_._2.key===key).map(_._1).getOrElse(""))
        .yLabel("Number of Teams")
        .yGrid()
        .xAxis(tickCount=Some(10))
        .yAxis()
         .render(Extent(width, height))
         .write(file)
      Ok(IOUtils.toByteArray(file.toURI)).as("image/png")
    }
  }
}
