package jobs

import java.time.LocalDate

import javax.inject.Inject
import akka.actor.Actor
import com.fijimf.deepfij.schedule.services.ScheduleUpdateService
import jobs.ScheduleUpdater.Update
import play.api.Logger

class ScheduleUpdater @Inject()(svc: ScheduleUpdateService) extends Actor  {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger: Logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case str:String=>
      val mysender=sender()
      svc.update(str).onComplete(mysender ! _)
  }
}

object ScheduleUpdater {

  final case class Update(dates: Option[List[LocalDate]] = None, sendEmail:Boolean=false)

  def forAll: Update = Update()

  def forNow: Update = Update(Some((-1).to(1).toList.map(LocalDate.now().plusDays(_))))

  def forDailyUpdate: Update = Update(Some((-7).to(7).toList.map(LocalDate.now().plusDays(_))))
val name = "schedule-updater"
}

