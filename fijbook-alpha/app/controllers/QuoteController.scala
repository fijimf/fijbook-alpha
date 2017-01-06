package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.{Quote, ScheduleDAO}
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import utils.DefaultEnv

import scala.util.Random

class QuoteController @Inject()(val teamDao: ScheduleDAO, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {

  val DEFAULT_QUOTE = Map("quote" -> "Fridge rules.", "url" -> "")

  def random = Action.async {
    teamDao.listQuotes.map(qs => {
      val rs = qs.filter(_.key.isEmpty)
      if (rs.isEmpty) {
        Ok(Json.toJson(DEFAULT_QUOTE))
      } else {
        Ok(Json.toJson(randomQuote(rs)))
      }
    })
  }

  def keyed(key: String) = Action.async {
    teamDao.listQuotes.map(allQuotes => {
      val matchedQuotes = allQuotes.filter(_.key.contains(key))
      val unkeyedQuotes = allQuotes.filter(_.key.isEmpty)
      if (matchedQuotes.isEmpty || Random.nextDouble() > 0.5) {
        if (unkeyedQuotes.isEmpty) {
          Ok(Json.toJson(DEFAULT_QUOTE))
        } else {
          val n = Random.nextInt(unkeyedQuotes.size)
          val quote = unkeyedQuotes(n)
          Ok(Json.toJson(Map("quote" -> quote.quote, "url" -> quote.url.getOrElse(""))))
        }
      } else {
        Ok(Json.toJson(randomQuote(matchedQuotes)))
      }
    })

  }

  private def randomQuote(unkeyedQuotes: List[Quote]) = {
    val quote = unkeyedQuotes(Random.nextInt(unkeyedQuotes.size))
    Map("quote" -> quote.quote, "url" -> quote.url.getOrElse(""))
  }

}