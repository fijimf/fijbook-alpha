package modules

import akka.actor.Props
import akka.contrib.throttle.Throttler.Rate
import akka.contrib.throttle.TimerBasedThrottler
import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, ScheduleDAOImpl}
import com.fijimf.deepfij.models.services._
import com.fijimf.deepfij.scraping.ScrapingActor
import com.fijimf.deepfij.scraping.nextgen.SuperScrapeActor
import com.fijimf.deepfij.stats.spark.SpStatActor
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.duration._


class ScrapingModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[SuperScrapeActor]("super-scraper")
    bindActor[ScrapingActor]("data-load-actor")
    bindActor[TimerBasedThrottler]("throttler", p => Props(classOf[TimerBasedThrottler], Rate(2, 1.second)))
    bindActor[SpStatActor]("spark-stats")
    bind[ScheduleDAO].to[ScheduleDAOImpl]
    bind[ScheduleUpdateService].to[ScheduleUpdateServiceImpl]
    bind[ComputedStatisticService].to[ComputedStatisticServiceImpl]
    bind[MemoryMonitorService].to[MemoryMonitorServiceImpl]
    bind[GamePredictorService].to[GamePredictorServiceImpl]
  }

}
