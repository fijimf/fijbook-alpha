package controllers

import java.time.LocalDateTime

import com.fijimf.deepfij.models.{Qotd, Season, Team, TeamDAO}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import forms.{CreateQuoteForm, CreateSeasonForm, EditTeamForm}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future
import scala.util.{Failure, Success}

class EditController @Inject()(val teamDao:TeamDAO, silhouette: Silhouette[DefaultEnv],  val messagesApi: MessagesApi) extends Controller with I18nSupport {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger = Logger(getClass)

  def saveTeam= silhouette.SecuredAction.async { implicit request =>
    EditTeamForm.form.bindFromRequest.fold(
      form => {
        val formId: Int = form("id").value.getOrElse("0").toInt
        val fot: Future[Option[Team]] = teamDao.find(formId)
        fot.map(ot=> ot match {
          case Some(t) => BadRequest(views.html.admin.team_edit(request.identity, t, form))
          case None => Redirect(routes.EditController.browseTeams()).flashing("error" -> ("Bad request with an unknown id: " + form("id")))
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
        teamDao.save(t).map(i=>Redirect(routes.EditController.browseTeams()).flashing("info" -> ("Saved " + data.name)))
      }
    )
  }

  def editTeam(id:Long) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.find(id).map{
      case Some(t)=> Ok(views.html.admin.team_edit(rs.identity,t,EditTeamForm.form.fill(EditTeamForm.team2Data(t))))
      case None=>Redirect(routes.EditController.browseTeams()).flashing("warn"->("No team found with id "+id))
    }
   }

  def browseTeams() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.list.map(ot=>Ok(views.html.admin.team_browse(rs.identity,ot.sortBy(_.name))))
  }

  def createSeason() = silhouette.SecuredAction.async{ implicit rs=>
    Future.successful(Ok(views.html.admin.season_create(rs.identity, CreateSeasonForm.form)))

  }

  def saveSeason() = silhouette.SecuredAction.async { implicit request =>
    CreateSeasonForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
          Future.successful(BadRequest(views.html.admin.season_create(request.identity, form)))
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

  def createQuote() = silhouette.SecuredAction.async{ implicit rs=>
    Future.successful(Ok(views.html.admin.qotd_create(rs.identity, CreateQuoteForm.form)))
  }

  def saveQuote() = silhouette.SecuredAction.async { implicit request =>
    CreateQuoteForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
          Future.successful(BadRequest(views.html.admin.qotd_create (request.identity, form)))
      },
      data => {
        val q = Qotd(data.id, data.quote, data.source, data.url)
        val future: Future[Int] = teamDao.saveQuote(q)
        future.onComplete{
          case Success(i)=> logger.info("Hooray")
          case Failure(thr)=> logger.error("Boo", thr)
        }
        future.map(i=>Redirect(routes.AdminController.index()).flashing("info" -> ("Created " + data.quote)))
      }
    )
  }
}