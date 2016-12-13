package controllers

import java.time.LocalDate

import com.fijimf.deepfij.models.{ScheduleDAO, Team}
import com.fijimf.deepfij.stats.WonLost
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

/**
  * Created by jimfrohnhofer on 12/11/16.
  */
class StatsController @Inject()(val teamDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {


  def win= silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          new WonLost(sch).
          val wl: Map[LocalDate, Map[Team, Accumulator]] = WonLost(sch)
          val date = wl.keys.maxBy(_.toEpochDay)
          wl(date).foreach((tuple: (Team, Accumulator)) =>  println(tuple._1.name+" "+WonLost.wins(tuple._2)))
          Redirect(routes.IndexController.index()).flashing("info" -> "Check logs display not yet impleneted")
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }

}
