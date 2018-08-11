package controllers

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.react._
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Quote, QuoteVote, _}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.Random

class QuoteController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with WithDao with ReactEnricher {

  val DEFAULT_QUOTE = Quote(-1L, "Fridge rules.", None, None, None)

  def random = Action.async {
    dao.listQuotes.map(qs => {
      val rs = qs.filter(_.key.isEmpty)
      if (rs.isEmpty) {
        Ok(Json.toJson(DEFAULT_QUOTE))
      } else {
        Ok(Json.toJson(randomQuote(rs)))
      }
    })
  }

  def keyed(key: String) = Action.async {
    dao.listQuotes.map(allQuotes => {
      val matchedQuotes = allQuotes.filter(_.key.contains(key))
      val unkeyedQuotes = allQuotes.filter(_.key.isEmpty)
      if (matchedQuotes.isEmpty || Random.nextDouble() > 0.5) {
        if (unkeyedQuotes.isEmpty) {
          Ok(Json.toJson(DEFAULT_QUOTE))
        } else {
          val n = Random.nextInt(unkeyedQuotes.size)
          val quote = unkeyedQuotes(n)
          Ok(Json.toJson(quote))
        }
      } else {
        Ok(Json.toJson(randomQuote(matchedQuotes)))
      }
    })

  }

  def likequote(id: Long) = silhouette.SecuredAction.async { implicit rs: SecuredRequest[DefaultEnv, AnyContent] =>
    dao.saveQuoteVote(QuoteVote(0L, id, rs.identity.userID.toString, LocalDateTime.now())).flatMap(_ => {
      loadDisplayUser(rs).map(du => {
        Ok(Json.toJson(du))
      })
    })
  }

  private def randomQuote(unkeyedQuotes: List[Quote]): Quote = {
    unkeyedQuotes(Random.nextInt(unkeyedQuotes.size))
  }

}