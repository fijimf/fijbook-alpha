package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Game, ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.nav.{Breadcrumbs, DeepFijPageMetaData, StandardNavBar}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class IndexController @Inject()(val teamDao: ScheduleDAO,val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {


  def index = silhouette.UserAwareAction.async { implicit rs =>
    val today =  LocalDate.now()
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val yesterdayGames = sch.map(_.games.filter(_.date == yesterday)).getOrElse(List.empty[Game])
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])
      Ok(views.html.frontPage(indexPageMetaData(rs), sch, yesterdayGames, todayGames))

    })
  }

  def redirect = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Redirect(routes.IndexController.index())
    }
  }

  def about() =  silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Ok(views.html.about(aboutPageMetaData(rs)))
    }
  }

  def aboutPageMetaData(rs:UserAwareRequest[DefaultEnv,_]):DeepFijPageMetaData ={
    val maybeUser = rs.identity
    DeepFijPageMetaData(
      "deepfij",
      "Deep Fij",
      "About",
      Some(Breadcrumbs.base),
      StandardNavBar.active("about",maybeUser),
      maybeUser
    )
  }

  def indexPageMetaData(rs:UserAwareRequest[DefaultEnv,_]):DeepFijPageMetaData = {
    val maybeUser = rs.identity
    val today = LocalDate.now()
    DeepFijPageMetaData(
      "deepfij",
      "Deep Fij",
      today.toString,
      Some(Breadcrumbs.base),
      StandardNavBar.active("no-active", maybeUser),
      maybeUser
    )
  }
}
