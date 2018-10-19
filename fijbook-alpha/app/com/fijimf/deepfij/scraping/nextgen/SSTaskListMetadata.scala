package com.fijimf.deepfij.scraping.nextgen
import java.time.LocalDateTime


final case class SSTaskListMetadata(id: String, tasks: List[SSTaskMetaData]) {

  val (status:String, time:LocalDateTime) = {
    tasks.headOption match {
      case Some(s:SSRunningTaskMetadata[_])=>("Running", s.startedAt)
      case Some(s:SSCompletedTaskMetadata[_])=>("Completed",s.completedAt)
      case Some(s:SSFailedTaskMetadata[_])=>("Failed",s.completedAt)
      case Some(s:SSAbortedTaskMetadata[_])=>("Aborted",s.abortedAt)
      case None=>("Unknown",LocalDateTime.of(1900,1,1,0,0,0,0))
    }
  }
}