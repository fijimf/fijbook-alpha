package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{GamePredictorService, ScheduleSerializer}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future

class SparkStatController@Inject()(
                                    val controllerComponents: ControllerComponents,
                                    val dao: ScheduleDAO,
                                    val silhouette: Silhouette[DefaultEnv]
                                  )
  extends BaseController with I18nSupport {
  val log = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  def update() = silhouette.SecuredAction.async { implicit rs =>
   
    Future.successful(
      ScheduleSerializer.readLatestSnapshot().map(u=>{
        Some(0)
              }) match {
        case Some(m)=> Ok(s"It worked\n$m")
        case None => Ok("Boo")
      }
     
    )
  }


}
