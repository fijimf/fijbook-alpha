package modules

import akka.actor.Props
import akka.contrib.throttle.Throttler.Rate
import akka.contrib.throttle.TimerBasedThrottler
import cats.effect.{ContextShift, IO}
import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, ScheduleDAOImpl}
import com.fijimf.deepfij.scraping.model.ScrapingActor
import com.fijimf.deepfij.scraping.nextgen.SuperScrapeActor
import com.google.inject.AbstractModule
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import doobie.util.{ExecutionContexts, transactor}
import net.codingwell.scalaguice.ScalaModule
import org.h2.util.New
import play.api.{Configuration, Environment}
import play.api.libs.concurrent.AkkaGuiceSupport

class DoobieTransactorModule extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(bind[TransactorCtx].to[TransactorCtxImpl])
  }
}

class TransactorCtxImpl extends TransactorCtx {
  import doobie.util.ExecutionContexts
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",           // driver classname
  "jdbc:postgresql://localhost:5432/deepfijdb",    // connect URL (driver-specific)
  "fijuser",                          // user
  "mut()mb()",                        // password
  ExecutionContexts.synchronous // just for testing
  )
}