package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.{ScheduleRepository, UserRepository}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller, Result}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareAction
import utils._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SchemaHelper @Inject()(val silhouette: Silhouette[DefaultEnv],repo: ScheduleRepository, userRepo: UserRepository) extends Controller {

  def dumpSchema() = silhouette.UserAwareAction.async { implicit rs =>
    repo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }

  def dropCreateSchema() = silhouette.SecuredAction.async { implicit rs =>
    if (rs.identity.email.forall(_=="fijimf@gmail.com")) {
      for (
        u1 <- repo.dropSchema();
        u2 <- repo.createSchema()
      ) yield {
        Redirect("/deepfij/admin").flashing("level" -> "info", "message" -> "Schedule schema was dropped and recreated")
      }
    } else {
      Future{Redirect("/deepfij/admin").flashing("level" -> "error", "message" -> "You are not authorized to recreate schema")}
    }
  }

  def dumpUserSchema() = silhouette.UserAwareAction.async { implicit rs =>
    userRepo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }

  def dropCreateUserSchema() = silhouette.SecuredAction.async { implicit rs =>
    if (rs.identity.email.forall(_=="fijimf@gmail.com")) {
      for (
        u1 <- userRepo.dropSchema();
        u2 <- userRepo.createSchema()
      ) yield {
        Redirect("/deepfij/admin").flashing("level"->"info", "message"->"User schema was dropped and recreated")
      }
    } else {
      Future{Redirect("/deepfij/admin").flashing("level" -> "error", "message" -> "You are not authorized to recreate schema")}
    }

  }


}
