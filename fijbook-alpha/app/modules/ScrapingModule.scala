package modules

import akka.actor.Props
import akka.contrib.throttle.Throttler.Rate
import akka.contrib.throttle.TimerBasedThrottler
import com.fijimf.deepfij.models.services._
import com.fijimf.deepfij.models.{AuthTokenDAO, AuthTokenDAOImpl, ScheduleDAO, ScheduleDAOImpl}
import com.fijimf.deepfij.scraping.modules.scraping.ScrapingActor
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.duration._


class ScrapingModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScrapingActor]("data-load-actor")
    bindActor[TimerBasedThrottler]("throttler",p=> Props(classOf[TimerBasedThrottler], Rate(2 , 1.second)))
    bind[ScheduleDAO].to[ScheduleDAOImpl]
    bind[ScheduleUpdateService].to[ScheduleUpdateServiceImpl]
    bind[StatisticWriterService].to[StatisticWriterServiceImpl]
    bind[MemoryMonitorService].to[MemoryMonitorServiceImpl]
  }

}
