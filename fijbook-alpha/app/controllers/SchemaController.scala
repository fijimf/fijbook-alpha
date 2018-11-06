package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import javax.inject.Inject
import com.fijimf.deepfij.models.{ScheduleRepository, UserRepository}
import play.api.mvc._
import com.mohiva.play.silhouette.api.Silhouette
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.basic.BasicProfile
import controllers.silhouette.utils._

import scala.concurrent.{ExecutionContext, Future}

class SchemaController @Inject()(
                                  val controllerComponents:ControllerComponents,
                                  val silhouette: Silhouette[DefaultEnv],
                                  val repo: ScheduleRepository,
                                  val dao:ScheduleDAO,
                                  val userRepo: UserRepository)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher {

  def showSchema() = silhouette.UserAwareAction.async { implicit rs =>
    for {
      du<-loadDisplayUser(rs)
      qw<-getQuoteWrapper(du)
      dropCreateStatements<-repo.dumpSchema()
    } yield {
      Ok (views.html.admin.showSchema(du,qw,dropCreateStatements))
    }
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

  def showUserSchema() = silhouette.UserAwareAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      dropCreateStatements <- userRepo.dumpSchema()
    } yield {
      Ok(views.html.admin.showSchema(du, qw, dropCreateStatements))
    }
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
