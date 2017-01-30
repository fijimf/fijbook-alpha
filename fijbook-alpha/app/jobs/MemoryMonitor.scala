package jobs

import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.MemoryMonitorService

class MemoryMonitor @Inject()(svc: MemoryMonitorService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case MemoryMonitor.CheckMemory => svc.check()
  }

}

object MemoryMonitor {

  case object CheckMemory

}


