package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.Quote
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.ExecutionContext
import scala.util.Random

class QuoteController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController {

  val DEFAULT_QUOTE = Map("quote" -> "Fridge rules.", "url" -> "", "source" -> "")

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
          Ok(Json.toJson(Map("quote" -> quote.quote, "url" -> quote.url.getOrElse(""), "source" -> quote.source.getOrElse(""))))
        }
      } else {
        Ok(Json.toJson(randomQuote(matchedQuotes)))
      }
    })

  }

  private def randomQuote(unkeyedQuotes: List[Quote]) = {
    val quote = unkeyedQuotes(Random.nextInt(unkeyedQuotes.size))
    Map("quote" -> quote.quote, "url" -> quote.url.getOrElse(""), "source" -> quote.source.getOrElse(""))
  }

}