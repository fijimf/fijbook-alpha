package controllers

import com.fijimf.deepfij.models.QuoteVote
import com.fijimf.deepfij.models.react.{DisplayLink, DisplayUser}
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.silhouette.utils.DefaultEnv
import org.apache.commons.lang3.StringUtils
import play.api.mvc.AnyContent

import scala.concurrent.{ExecutionContext, Future}

trait ReactEnricher {

  self:WithDao=>

  def loadDisplayUser(rs: UserAwareRequest[DefaultEnv, AnyContent])(implicit executionContext: ExecutionContext):Future[DisplayUser]={
    val favorites = loadUserFavorites(rs)

    val likedQuotes = loadUserLikedQuotes(rs)
    for {
      f<-favorites
      l<-likedQuotes
    } yield {
      DisplayUser(rs.identity, rs.identity.exists(_.isDeepFijAdmin), rs.identity.isDefined,f,l.map(_.quoteId.toInt))
    }
  }

   private def loadUserLikedQuotes(rs: UserAwareRequest[DefaultEnv, AnyContent]): Future[List[QuoteVote]] = {
     import scala.concurrent.duration._
    val lookupName = rs.identity.map(_.name) match {
      case None => rs.connection.remoteAddressString
      case Some(n) if StringUtils.isBlank(n) => rs.connection.remoteAddressString
      case Some(n) => n
    }
    dao.findQuoteVoteByUser(lookupName, 24.hours)
  }

  private def loadUserFavorites(rs: UserAwareRequest[DefaultEnv, AnyContent])(implicit executionContext: ExecutionContext): Future[List[DisplayLink]] = {
    rs.identity match {
      case Some(u) =>
        dao.findFavoriteLinksByUser(u.userID.toString).map(_.map(fl => DisplayLink(fl.displayAs, fl.link, "fa-star")))
      case None =>
        Future.successful(List.empty[DisplayLink])
    }
  }
}
