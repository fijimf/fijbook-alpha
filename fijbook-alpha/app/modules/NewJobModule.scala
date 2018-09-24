package modules

import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, ScheduleDAOImpl}
import jobs._
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * The job module.
  */
class NewJobModule extends ScalaModule with AkkaGuiceSupport {

  def configure() = {
    bind[ScheduleDAO].to[ScheduleDAOImpl]
    bindActor[AuthTokenCleaner](AuthTokenCleaner.name)
    bind[NewScheduler].asEagerSingleton()
  }
}