package controllers

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.react._
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Quote, QuoteVote, _}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.libs.json.{Format, Json}
import play.api.mvc
import play.api.mvc.{AnyContent, BaseController, ControllerComponents}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class QuoteController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with WithDao with ReactEnricher {

  case class QuoteWrapper(quote: Quote, isLiked: Boolean, canVote: Boolean)

  implicit val formatsQuoteWrappers: Format[QuoteWrapper] = Json.format[QuoteWrapper]

  val missingQuote = QuoteWrapper(Quote(-1L, "Fridge rules.", None, None, None), isLiked = false, canVote = false)

  def random = silhouette.UserAwareAction.async { implicit req =>
    dao.listQuotes.flatMap(qs => {
      val rs = qs.filter(_.key.isEmpty)
      if (rs.isEmpty) {
        Future.successful(Ok(Json.toJson(missingQuote)))
      } else {
        quoteUserResponse(req, randomQuote(rs))
      }
    })
  }


  def keyed(key: String) = silhouette.UserAwareAction.async { implicit req =>
    dao.listQuotes.flatMap(allQuotes => {
      val matchedQuotes = allQuotes.filter(_.key.contains(key))
      val unkeyedQuotes = allQuotes.filter(_.key.isEmpty)
      if (matchedQuotes.isEmpty || Random.nextDouble() > 0.5) {
        if (unkeyedQuotes.isEmpty) {
          Future.successful(Ok(Json.toJson(missingQuote)))
        } else {
          quoteUserResponse(req, unkeyedQuotes(Random.nextInt(unkeyedQuotes.size)))
        }
      } else {
        Future(Ok(Json.toJson(randomQuote(matchedQuotes))))
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

  private def quoteUserResponse(req: UserAwareRequest[DefaultEnv, AnyContent], quote: Quote): Future[mvc.Result] = {
    req.identity match {
      case Some(u) =>
        dao.findQuoteVoteByUser(u.userID.toString, 7.days)
          .map(!_.exists(_.quoteId == quote.id))
          .map(canVote => Ok(Json.toJson(QuoteWrapper(quote, isLiked = !canVote, canVote = canVote))))
      case None =>
        Future.successful(Ok(Json.toJson(QuoteWrapper(quote, isLiked = false, canVote = false))))
    }
  }

}