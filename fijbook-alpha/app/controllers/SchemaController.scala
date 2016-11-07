package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.{ScheduleRepository, UserRepository}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller, Result}
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareAction
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.profile.BasicProfile
import utils._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SchemaController @Inject()(val silhouette: Silhouette[DefaultEnv],repo: ScheduleRepository, userRepo: UserRepository) extends Controller {

  def dumpSchema() = silhouette.UserAwareAction.async { implicit rs =>
    repo.dumpSchema().map(tup =>Ok (views.html.admin.showSchema(tup,rs.identity)))
  }

  def dropCreateSchema() = silhouette.SecuredAction.async { implicit rs =>
    if (rs.identity.email.forall(_=="fijimf@gmail.com")) {
      for (
        u1 <- repo.dropSchema();
        u2 <- repo.createSchema()
      ) yield {
        Redirect("/deepfij/admin").flashing( "info" -> "Schedule schema was dropped and recreated")
      }
    } else {
      Future{Redirect("/deepfij/admin").flashing("error" -> "You are not authorized to recreate schema")}
    }
  }

  def dumpUserSchema() = silhouette.UserAwareAction.async { implicit rs =>
    userRepo.dumpSchema().map(tup =>Ok (views.html.admin.showSchema(tup, rs.identity)))
  }

  def dropCreateUserSchema() = silhouette.SecuredAction.async { implicit rs =>
    if (rs.identity.email.forall(_=="fijimf@gmail.com")) {
      for (
        u1 <- userRepo.dropSchema();
        u2 <- userRepo.createSchema()
      ) yield {
        Redirect("/deepfij/admin").flashing("info"->"User schema was dropped and recreated")
      }
    } else {
      Future{Redirect("/deepfij/admin").flashing( "error" -> "You are not authorized to recreate schema")}
    }

  }


}

object PrintSchema {
  def main(args: Array[String]): Unit = {
    val dbConfigProvider: DatabaseConfigProvider = new DatabaseConfigProvider {
      override def get[P <: BasicProfile]: DatabaseConfig[P] = DatabaseConfig.forConfig[BasicProfile]("slick.dbs.default").asInstanceOf[DatabaseConfig[P]]
    }
    new ScheduleRepository(dbConfigProvider).ddl.createStatements.foreach(s=>println(s+";"))
    new UserRepository(dbConfigProvider).ddl.createStatements.foreach(s=>println(s+";"))
  }
}
