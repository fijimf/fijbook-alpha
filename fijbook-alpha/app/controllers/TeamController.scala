package controllers

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.StatisticWriterService
import com.fijimf.deepfij.stats.{Model, Stat}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.concurrent.Future

class TeamController @Inject()(val teamDao: ScheduleDAO, val statWriterService: StatisticWriterService, cache: CacheApi, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def team(key: String) = silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().flatMap(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          val t: Team = sch.keyTeam(key)
          val stats = loadTeamStats(t, sch)
          stats.map(lstat=>{
            Ok(views.html.data.team(request.identity, t, sch, sortedSchedules.tail, lstat))
          })
        }
        case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded"))
      }

    })

  }

  def loadTeamStats(t: Team, sch:Schedule): Future[List[ModelTeamContext]] = {
    val futures = List("won-lost","scoring","rpi","least-squares").map(loadTeamStats(t,_, sch))
    Future.sequence(futures).map(_.flatten)
  }

  def loadTeamStats(t: Team, modelKey: String, sch:Schedule): Future[Option[ModelTeamContext]] = {
    statWriterService.lookupModel(modelKey) match {
      case Some(model) =>
        cache.get[Map[String, List[StatValue]]]("model." + modelKey) match {
          case Some(byKey) => Future.successful(Some(ModelTeamContext(t, model, model.stats, byKey, sch.teamsMap)))
          case None =>
            teamDao.loadStatValues(modelKey).map(stats => {
              val byKey = stats.groupBy(_.statKey)
              cache.set("model." + modelKey, byKey, 15.minutes)
              Some(ModelTeamContext(t, model, model.stats, byKey, sch.teamsMap))
            })
          case None => Future.successful(Option.empty[ModelTeamContext])
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
            case t :: Nil => Redirect(routes.TeamController.team(t.key))
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

  def values(stat: Stat[_]): List[(LocalDate, Int, StatValue)] = {
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
    if (teamByDate.isEmpty) {
      List.empty[(LocalDate, Int, StatValue)]
    } else {
      val firstDate = teamByDate.minBy(_._1.toEpochDay)._1
      val latest = teamByDate.maxBy(_._1.toEpochDay)
      val prev = teamByDate.filter(_._1.isBefore(latest._1)).maxBy(_._1.toEpochDay)
      val weekAgo = teamByDate.filter(_._1.isBefore(latest._1.plusDays(-6))).maxBy(_._1.toEpochDay)

      val best = teamByDate.filter(_._2 == teamByDate.minBy(_._2)._2).maxBy(_._1.toEpochDay)
      val worst = teamByDate.filter(_._2 == teamByDate.minBy(_._2)._2).maxBy(_._1.toEpochDay)
      List(latest, prev, weekAgo, best, worst)
    }
  }
}
