package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models.Game
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.Controller
import utils.DefaultEnv

/**
  * Created by jimfrohnhofer on 7/18/17.
  */
class S3BlockController  @Inject()(val teamDao: ScheduleDAO,val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {
  def block(slug:String) = silhouette.UserAwareAction.async { implicit rs =>
    val today =  LocalDate.now()
    val yesterday = today.minusDays(1)
    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      val sch = sortedSchedules.headOption
      val yesterdayGames = sch.map(_.games.filter(_.date == yesterday)).getOrElse(List.empty[Game])
      val todayGames = sch.map(_.games.filter(_.date == today)).getOrElse(List.empty[Game])
      Ok(views.html.frontPage(rs.identity, today, sch, yesterdayGames, todayGames))

    })
  }
}
