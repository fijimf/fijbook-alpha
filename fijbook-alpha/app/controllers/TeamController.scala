package controllers

import com.fijimf.deepfij.models.ScheduleDAO
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

class TeamController @Inject()(val teamDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def team(key: String) = silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          Ok(views.html.data.team(request.identity, sch.keyTeam(key), sch, sortedSchedules.tail))
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

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
            case Nil=>
              val columns = sch.teams.sortBy(_.name).grouped((sch.teams.size + 3) / 4).toList
              Ok(views.html.data.teams(request.identity, columns)).flashing("info"->("No matching teams for query string '"+q+"'"))
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


}