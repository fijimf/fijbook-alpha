package controllers

import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import forms.EditJobForm
import javax.inject.Inject
import jobs.DeepFijQuartzSchedulerExtension
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}


class JobControlController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val system: ActorSystem,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {


  def createJob() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => Future.successful(Ok(views.html.admin.createJob(d, q, EditJobForm.form)))
    }
  }

  def browseJobs() = silhouette.SecuredAction.async { implicit rs =>
    val fq = DeepFijQuartzSchedulerExtension(system)
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.listJobs.map(qs => Ok(views.html.admin.browseJobs(d, q, qs, fq)))
    }
  }

  def saveJob() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => EditJobForm.form.bindFromRequest.fold(
      form => {
        Future.successful(BadRequest(views.html.admin.createJob(d, q, form)))
      },
      data => {
        val q = Job(data.id, data.name, data.description, data.cronSchedule, data.timezone, data.actorClass, data.message, data.timeout, data.isEnabled, LocalDateTime.now())
        val future: Future[Job] = dao.saveJob(q)
        future.map(i => Redirect(routes.JobControlController.browseJobs()).flashing("info" -> ("Saved job " + data.name)))
      }
    )
    }
  }

  def editJob(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, qq) => dao.findJobById(id).map {
      case Some(j) => Ok(views.html.admin.createJob(d, qq, EditJobForm.form.fill(EditJobForm.Data(j.id, j.name, j.description, j.cronSchedule, j.timezone, j.actorClass, j.message, j.timeout, j.isEnabled))))
      case None => Redirect(routes.JobControlController.browseJobs()).flashing("warn" -> ("No Job found with id " + id))
    }
    }
  }

  def deleteJob(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.deleteJob(id).map(n => Redirect(routes.JobControlController.browseJobs()).flashing("info" -> ("Job " + id + " deleted")))
    }
  }

}