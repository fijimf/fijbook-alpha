package jobs

import java.time.LocalDate
import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import jobs.ScheduleUpdater.Update

class ScheduleUpdater @Inject()(svc: ScheduleUpdateService) extends Actor  {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case Update(od, sendEmail)=> svc.update(od, sendEmail)

  }
}

object ScheduleUpdater {

  case class Update(dates: Option[List[LocalDate]] = None, sendEmail:Boolean=false)

  def forAll = Update()

  def forNow = Update(Some((-1).to(1).map(n => LocalDate.now().plusDays(n)).toList))

  def forDailyUpdate = Update(Some((-7).to(7).map(n => LocalDate.now().plusDays(n)).toList))

}

