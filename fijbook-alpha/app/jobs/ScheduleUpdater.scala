package jobs

import java.time.LocalDate
import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import jobs.ScheduleUpdater.Update

class ScheduleUpdater @Inject()(svc: ScheduleUpdateService) extends Actor  {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case Update(od, sendEmail)=> svc.updateSeason(od, sendEmail)
  }
}

object ScheduleUpdater {
  val nowDays = (-1).to(1).toList
  val dailyDays = (-7).to(7).toList

  case class Update(dates: Option[List[LocalDate]] = None, sendEmail:Boolean=false)

  def forAll = Update()

  def forNow() = {
    val d = LocalDate.now()
    Update(Some(nowDays.map(d.plusDays(_))))
  }

  def forDailyUpdate() = {
    val d = LocalDate.now()
    Update(Some(dailyDays.map(d.plusDays(_))))
  }

}

