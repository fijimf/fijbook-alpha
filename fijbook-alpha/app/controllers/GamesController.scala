package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Game, GprCohort}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.ExecutionContext

class GamesController @Inject()(
                                 val controllerComponents:ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController {

val predictors = List(
  "naive-regression"->"Linear Regressor",
  "logistic-wp"->"Logistic Regressor (win %)",
  "logistic-x-margin-ties"->"Logistic Regressor (linear estimator)",
  "logistic-rpi121"->"Logistic Regressor (rpi 121)"
)

  def games(modelKey:Option[String]) = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])
      Ok(views.html.gamelist(rs.identity, today, sch, todayGames, yesterday, tomorrow, modelKey.getOrElse("naive-regression"),predictors))
    })
  }

  def gamesByDate(yyyymmdd:String, modelKey:Option[String]) = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE)
     val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])

      Ok(views.html.gamelist(rs.identity, today, sch, todayGames, yesterday, tomorrow,modelKey.getOrElse("naive-regression"),predictors))
    })
  }

  def gamesApi(yyyymmdd:String, modelKey:Option[String]) = silhouette.UserAwareAction.async { implicit rs =>
    val today = LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE)
    teamDao.loadSchedules().map(ss => {
      ss.sortBy(s => -s.season.year).headOption.map(sch => {
        Json.toJson(GprCohort(sch, today,modelKey.getOrElse("naive-regression")).toJson)
      }).getOrElse(Json.toJson(JsArray(Seq.empty)))
    }).map(js=>Ok(js))
  }


}
