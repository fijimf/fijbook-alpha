package controllers

import java.time.LocalDateTime

import com.fijimf.deepfij.models._
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import forms.{CreateAliasForm, CreateQuoteForm, CreateSeasonForm, EditTeamForm}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DataController @Inject()(val teamDao:ScheduleDAO, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger = Logger(getClass)

  def saveTeam= silhouette.SecuredAction.async { implicit request =>
    EditTeamForm.form.bindFromRequest.fold(
      form => {
        val formId: Int = form("id").value.getOrElse("0").toInt
        val fot: Future[Option[Team]] = teamDao.findTeamById(formId)
        fot.map(ot=> ot match {
          case Some(t) => BadRequest(views.html.admin.editTeam(request.identity, t, form))
          case None => Redirect(routes.DataController.browseTeams()).flashing("error" -> ("Bad request with an unknown id: " + form("id")))
        })
      },
      data => {
        val t = Team(
          data.id,
          data.key,
          data.name,
          data.longName,
          data.nickname,
          data.logoLgUrl,
          data.logoSmUrl,
          data.primaryColor,
          data.secondaryColor,
          data.officialUrl,
          data.officialTwitter,
          data.officialUrl,
          true,
          LocalDateTime.now(),
          request.identity.userID.toString)
        teamDao.saveTeam(t).map(i=>Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Saved " + data.name)))
      }
    )
  }

  def editTeam(id:Long) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.findTeamById(id).map{
      case Some(t)=> Ok(views.html.admin.editTeam(rs.identity,t,EditTeamForm.form.fill(EditTeamForm.team2Data(t))))
      case None=>Redirect(routes.DataController.browseTeams()).flashing("warn"->("No team found with id "+id))
    }
   }

  def browseTeams() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.listTeams.map(ot=>Ok(views.html.admin.browseTeams(rs.identity,ot.sortBy(_.name))))
  }

  def createSeason() = silhouette.SecuredAction.async{ implicit rs=>
    Future.successful(Ok(views.html.admin.createSeason(rs.identity, CreateSeasonForm.form)))

  }

  def saveSeason() = silhouette.SecuredAction.async { implicit request =>
    CreateSeasonForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
          Future.successful(BadRequest(views.html.admin.createSeason(request.identity, form)))
      },
      data => {
        val s = Season(data.id, data.year)
        val future: Future[Int] = teamDao.saveSeason(s)
        future.onComplete{
          case Success(i)=> logger.info("Hooray")
          case Failure(thr)=> logger.error("Boo", thr)
        }
        future.map(i=>Redirect(routes.AdminController.index()).flashing("info" -> ("Created empty season for " + data.year)))
      }
    )
  }

  def browseQuotes() = silhouette.SecuredAction.async{ implicit rs=>
   teamDao.listQuotes.map(qs=> Ok(views.html.admin.browseQuotes(rs.identity, qs)))
  }
  def createQuote() = silhouette.SecuredAction.async{ implicit rs=>
    Future.successful(Ok(views.html.admin.createQuote(rs.identity, CreateQuoteForm.form)))
  }

  def deleteQuote(id:Long) = silhouette.SecuredAction.async{ implicit rs=>
   teamDao.deleteQuote(id).map(n=>Redirect(routes.DataController.browseQuotes()).flashing("info"->("Quote " +id+" deleted")))
  }

  def editQuote(id:Long) = silhouette.SecuredAction.async{ implicit rs=>
    teamDao.findQuoteById(id).map {
      case Some(q) => Ok(views.html.admin.createQuote(rs.identity, CreateQuoteForm.form.fill(CreateQuoteForm.Data(id, q.quote, q.source, q.url))))
      case None => Redirect(routes.DataController.browseQuotes()).flashing("warn" -> ("No quote found with id " + id))
    }
  }

  def saveQuote() = silhouette.SecuredAction.async { implicit request =>
    CreateQuoteForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
          Future.successful(BadRequest(views.html.admin.createQuote (request.identity, form)))
      },
      data => {
        val q = Quote(data.id, data.quote, data.source, data.url)
        val future: Future[Int] = teamDao.saveQuote(q)
        future.onComplete{
          case Success(i)=> logger.info("Hooray")
          case Failure(thr)=> logger.error("Boo", thr)
        }
        future.map(i=>Redirect(routes.AdminController.index()).flashing("info" -> ("Created " + data.quote)))
      }
    )
  }

  def createAlias() = silhouette.SecuredAction.async{ implicit rs=>
    Future.successful(Ok(views.html.admin.createAlias(rs.identity, CreateAliasForm.form)))
  }
  def browseAliases() = silhouette.SecuredAction.async{ implicit rs=>
    teamDao.listAliases.map(qs=> Ok(views.html.admin.browseAliases(rs.identity, qs)))
  }

  def saveAlias() = silhouette.SecuredAction.async { implicit request =>
    CreateAliasForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.createAlias (request.identity, form)))
      },
      data => {
        val q = Alias(data.id, data.key, data.alias)
        val future: Future[Int] = teamDao.saveAlias(q)
        future.onComplete{
          case Success(i)=> logger.info("Hooray")
          case Failure(thr)=> logger.error("Boo", thr)
        }
        future.map(i=>Redirect(routes.AdminController.index()).flashing("info" -> ("Created " + data.alias)))
      }
    )
  }

  def editAlias(id: Long) = silhouette.SecuredAction.async{ implicit rs=>
    teamDao.findAliasById(id).map {
      case Some(q) => Ok(views.html.admin.createAlias(rs.identity, CreateAliasForm.form.fill(CreateAliasForm.Data(id, q.key, q.alias))))
      case None => Redirect(routes.DataController.browseAliases()).flashing("warn" -> ("No alias found with id " + id))
    }
  }

  def deleteAlias(id: Long) = silhouette.SecuredAction.async{ implicit rs=>
    teamDao.deleteAlias(id).map(n=>Redirect(routes.DataController.browseAliases()).flashing("info"->("Alias " +id+" deleted")))
  }
}