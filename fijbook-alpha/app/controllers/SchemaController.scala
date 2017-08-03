package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.{ScheduleRepository, UserRepository}
import play.api.mvc._
import com.mohiva.play.silhouette.api.Silhouette
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.basic.BasicProfile
import utils._

import scala.concurrent.{ExecutionContext, Future}

class SchemaController @Inject()(
                                  val controllerComponents:ControllerComponents,
                                  val silhouette: Silhouette[DefaultEnv],
                                  val repo: ScheduleRepository,
                                  val userRepo: UserRepository)(implicit ec: ExecutionContext)
  extends BaseController {

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
