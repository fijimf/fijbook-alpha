package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.Repo
import models.{ProjectRepo, TaskRepo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Result, Results, Action, Controller}

import scala.concurrent.ExecutionContext

class SchemaHelper @Inject()(repo: Repo) extends Controller {

  def dumpSchema() = Action.async { implicit rs =>
    repo.dumpSchema().map(tup=> tup._1.mkString("\n")+"\n==========\n"+tup._2.mkString("\n")).map(Ok (_))
  }
  def createSchema() = Action.async { implicit rs =>
    repo.createSchema().map(unit => Ok("Schema created"))
  }
  def dropSchema() = Action.async { implicit rs =>
    repo.dropSchema().map(unit => Ok("Schema dropped"))
  }

}
