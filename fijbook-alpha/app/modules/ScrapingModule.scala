package modules

import com.fijimf.deepfij.models.{ScheduleDAO, ScheduleDAOImpl}
import com.fijimf.deepfij.scraping.modules.scraping.ScrapingActor
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ScrapingModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScrapingActor]("data-load-actor")
    bind[ScheduleDAO].to[ScheduleDAOImpl]
  }

}
