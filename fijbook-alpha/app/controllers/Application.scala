package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.ScheduleRepository
import models.{ProjectRepo, TaskRepo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class Application @Inject()(  repo: ScheduleRepository)
                           extends Controller {



  def index = Action.async { implicit rs=>
      Future { Ok(views.html.index())}
  }

  def admin = Action.async { implicit rs=>
      Future { Ok(views.html.admin.index())}
  }

}
