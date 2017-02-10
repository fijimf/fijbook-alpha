package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Game, GprCohort, ScheduleDAO}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Controller
import utils.DefaultEnv

class GamesController @Inject()(val teamDao: ScheduleDAO, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {


  def games = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])
      Ok(views.html.gamelist(rs.identity, today, sch, todayGames, yesterday, tomorrow))
    })
  }

  def gamesByDate(yyyymmdd:String) = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE)
     val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])

      Ok(views.html.gamelist(rs.identity, today, sch, todayGames, yesterday, tomorrow))
    })
  }

  def gamesApi(yyyymmdd:String) = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE)
    teamDao.loadSchedules().map(ss => {
      ss.sortBy(s => -s.season.year).headOption.map(sch => {
        Json.toJson(GprCohort(sch, today).toJson)
      }).getOrElse(Json.toJson(JsArray(Seq.empty)))
    }).map(js=>Ok(js))
  }


}
