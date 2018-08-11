package controllers

import com.fijimf.deepfij.models.{QuoteVote, User}
import com.fijimf.deepfij.models.react.{DisplayLink, DisplayUser}
import com.mohiva.play.silhouette.api.actions.{SecuredAction, SecuredRequest, UserAwareRequest}
import controllers.silhouette.utils.DefaultEnv
import org.apache.commons.lang3.StringUtils
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.{ExecutionContext, Future}

trait ReactEnricher {

  self:WithDao=>

  def loadDisplayUser(rs: UserAwareRequest[DefaultEnv, AnyContent])(implicit executionContext: ExecutionContext):Future[DisplayUser]={
    val favorites = loadUserFavorites(rs.identity)

    val likedQuotes = loadUserLikedQuotes(rs.identity,rs)
    for {
      f<-favorites
      l<-likedQuotes
    } yield {
      DisplayUser(rs.identity, rs.identity.exists(_.isDeepFijAdmin), rs.identity.isDefined,f,l.map(_.quoteId.toInt))
    }
  }

  def loadDisplayUser(rs: SecuredRequest[DefaultEnv, AnyContent] )(implicit executionContext: ExecutionContext):Future[DisplayUser]={
    val favorites = loadUserFavorites(Some(rs.identity))

    val likedQuotes = loadUserLikedQuotes(Some(rs.identity),rs)
    for {
      f<-favorites
      l<-likedQuotes
    } yield {
      DisplayUser(Some(rs.identity), rs.identity.isDeepFijAdmin, isLoggedIn = true,f,l.map(_.quoteId.toInt))
    }
  }

  def loadDisplayUser(rs: Request[AnyContent])(implicit executionContext: ExecutionContext):Future[DisplayUser]={
    val favorites = loadUserFavorites(None)

    val likedQuotes = loadUserLikedQuotes(None, rs)
    for {
      f<-favorites
      l<-likedQuotes
    } yield {
      DisplayUser(None, isAdmin = false, isLoggedIn = false,f,l.map(_.quoteId.toInt))
    }
  }

   private def loadUserLikedQuotes(optUser:Option[User], req: Request[AnyContent]): Future[List[QuoteVote]] = {
     import scala.concurrent.duration._
    val lookupName = optUser.map(_.name) match {
      case None => req.connection.remoteAddressString
      case Some(n) if StringUtils.isBlank(n) => req.connection.remoteAddressString
      case Some(n) => n
    }
    dao.findQuoteVoteByUser(lookupName, 24.hours)
  }

  private def loadUserFavorites(optUser:Option[User])(implicit executionContext: ExecutionContext): Future[List[DisplayLink]] = {
    optUser match {
      case Some(u) =>
        dao.findFavoriteLinksByUser(u.userID.toString).map(_.map(fl => DisplayLink(fl.displayAs, fl.link, "fa-star")))
      case None =>
        Future.successful(List.empty[DisplayLink])
    }
  }
}
