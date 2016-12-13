package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import utils.DefaultEnv

import scala.concurrent.Future
import scala.util.Random

class QuoteController @Inject()(val teamDao: ScheduleDAO, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {


  def random = Action.async {
    teamDao.listQuotes.map(qs => {
      if (qs.isEmpty){
        Ok(Json.toJson(Map("quote" -> "Anger is an energy")))
      } else {
        val n = Random.nextInt(qs.size)
        val quote = qs(n)
        Ok(Json.toJson(Map("quote" -> quote.quote)))
      }
    })
  }
}
