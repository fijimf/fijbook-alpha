package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ComputedStatisticService
import com.fijimf.deepfij.stats.{Model, Stat}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._

class TeamController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val teamDao: ScheduleDAO,
                                val statWriterService: ComputedStatisticService,
                                cache: AsyncCacheApi,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def team(key: String, season: Option[Int]) = silhouette.UserAwareAction.async { implicit request =>
    teamDao.loadSchedules().flatMap(ss => {
      if (ss.isEmpty) {
        Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> "No schedules are loaded."))
      } else {
        val year = season match {
          case Some(y) => y
          case None => ss.map(_.season.year).max
        }
        ss.find(_.season.year == year) match {
          case Some(sch) => {
            val t: Team = sch.keyTeam(key)
            val stats = loadTeamStats(t, sch)
            stats.map(lstat => {
              Ok(views.html.data.team(request.identity, t, sch, lstat))
            })
          }
          case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> s"No schedule found for year $year"))
        }
      }
    })
  }

  def teamGames(key: String) = silhouette.UserAwareAction.async { implicit request =>
    teamDao.loadSchedules().map(f => {
      f.flatMap(s => {
        s.teams.find(_.key == key) match {
          case Some(team) => getTeamGames(s, team)
          case None => List.empty[JsObject]
        }
      })
    }).map(l => JsArray.apply(l)).map(Ok(_))
  }


  private def getTeamGames(s: Schedule, team: Team): List[JsObject] = {
    val year = s.season.year
    s.gameResults.flatMap {
      case (g, Some(r)) if g.homeTeamId == team.id =>
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
      case (g, Some(r)) if g.awayTeamId == team.id =>
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
    val futures = List("won-lost", "scoring", "rpi", "least-squares").map(loadTeamStats(t, _, sch))
    Future.sequence(futures).map(_.flatten)
  }

  def loadTeamStats(t: Team, modelKey: String, sch: Schedule): Future[Option[ModelTeamContext]] = {
    statWriterService.lookupModel(modelKey) match {
      case Some(model) =>
        cache.get[Map[String, List[StatValue]]]("model." + modelKey).flatMap {
          case Some(byKey) => Future.successful(Some(ModelTeamContext(t, model, model.stats, byKey, sch.teamsMap)))
          case None =>
            teamDao.loadStatValues(modelKey, sch.season.startDate, sch.season.endDate).map(stats => {
              val byKey = stats.groupBy(_.statKey)
              cache.set("model." + modelKey, byKey, 15.minutes)
              Some(ModelTeamContext(t, model, model.stats, byKey, sch.teamsMap))
            }
            )
        }
      case None => Future.successful(Option.empty[ModelTeamContext])
    }
  }

  def teams(q: String) = silhouette.UserAwareAction.async { implicit request =>
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          val matches = sch.teams.filter(t => {
            val qryStr = q.trim.toUpperCase
            t.name.toUpperCase.contains(qryStr) || t.longName.toUpperCase.contains(qryStr) || t.nickname.toUpperCase.contains(qryStr)
          })
          matches match {
            case Nil =>
              val columns = sch.teams.sortBy(_.name).grouped((sch.teams.size + 3) / 4).toList
              Ok(views.html.data.teams(request.identity, columns)).flashing("info" -> ("No matching teams for query string '" + q + "'"))
            case t :: Nil => Redirect(routes.TeamController.team(t.key, None))
            case lst =>
              val columns = lst.sortBy(_.name).grouped((lst.size + 3) / 4).toList
              Ok(views.html.data.teams(request.identity, columns))
          }
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }
    })
  }

  def conference(key: String) = silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          val c = sch.conferenceKeyMap(key)
          Ok(views.html.data.conference(request.identity, c, sch.conferenceStandings(c), sch.interConfRecord(c), sch.nonConferenceSchedule(c), sch.conferenceSchedule(c), sch))
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }

  def conferences() = silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          val cmap = sch.conferences.map(c => c -> sch.conferenceStandings(c)).sortBy(_._1.name)
          Ok(views.html.data.conferences(request.identity, cmap))
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }


}


case class ModelTeamContext(team: Team, model: Model[_], stats: List[Stat[_]], xs: Map[String, List[StatValue]], teamMap: Map[Long, Team]) {
  def modelName = model.name

  def modelKey = model.key

  def values(stat: Stat[_]): List[(String, Boolean, (LocalDate, Int, StatValue))] = {
    val teamByDate: List[(LocalDate, Int, StatValue)] = xs.get(stat.key) match {
      case Some(lsv) =>
        lsv.groupBy(_.date)
          .mapValues(lsvd => {
            StatUtil.transformSnapshot(lsvd, (sv: StatValue) => teamMap(sv.teamID), stat.higherIsBetter).find(_._3.id == team.id)
          }).toList.filter(_._2.isDefined).map { case (date: LocalDate, optTup: Option[(Int, StatValue, Team)]) =>
          val (rk, sv, t) = optTup.get
          (date, rk, sv)
        }
      case None => List.empty[(LocalDate, Int, StatValue)]
    }

    val firstDate = teamByDate match {
      case Nil => None
      case tbd => Some(tbd.minBy(_._1.toEpochDay))
    }
    val latest: Option[(String, Boolean, (LocalDate, Int, StatValue))] = teamByDate match {
      case Nil => None
      case tbd => Some((stat.name, true, tbd.maxBy(_._1.toEpochDay)))
    }

    val prev: Option[(String, Boolean, (LocalDate, Int, StatValue))] = latest.flatMap(lt => {
      teamByDate.filter(_._1.isBefore(lt._3._1)) match {
        case Nil => None
        case tbd => Some(("Prev", false, tbd.maxBy(_._1.toEpochDay)))
      }
    })
    val weekAgo: Option[(String, Boolean, (LocalDate, Int, StatValue))] = latest.flatMap(lt => {
      teamByDate.filter(_._1.isBefore(lt._3._1.plusDays(-6))) match {
        case Nil => None
        case tbd => Some(("Week Ago", false, tbd.maxBy(_._1.toEpochDay)))
      }
    })
    val best: Option[(String, Boolean, (LocalDate, Int, StatValue))] = teamByDate.filter(_._2 == teamByDate.minBy(_._2)._2) match {
      case Nil => None
      case tbd => Some(("Best", false, tbd.maxBy(_._1.toEpochDay)))
    }
    val worst: Option[(String, Boolean, (LocalDate, Int, StatValue))] = teamByDate.filter(_._2 == teamByDate.minBy(_._2)._2) match {
      case Nil => None
      case tbd => Some(("Worst", false, tbd.maxBy(_._1.toEpochDay)))
    }
    List(latest, prev, weekAgo, best, worst).flatten

  }
}
