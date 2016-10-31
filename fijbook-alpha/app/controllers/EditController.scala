package controllers

import com.fijimf.deepfij.models.{Team, TeamDAO}
import com.fijimf.deepfij.scraping.modules.scraping.requests.TeamDetail
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.{Await, Future}

class EditController @Inject()(val teamDao:TeamDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger = Logger(getClass)

  def saveTeam() = play.mvc.Results.TODO

  def editTeam(id:Long) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.find(id).map(ot=>Ok(views.html.admin.team_edit(rs.identity,ot)))
  }

  def browseTeams() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")

    teamDao.list.map(ot=>Ok(views.html.admin.team_browse(rs.identity,ot.sortBy(_.name))))
  }

}