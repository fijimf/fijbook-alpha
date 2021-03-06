package com.fijimf.deepfij.scraping.nextgen

import java.time.{Duration=>JDuration, LocalDateTime}


sealed trait SSTaskMetaData {
 def elapsedTime():JDuration
}

object SSTaskMetaData {
 val uninitializedRunningTask: SSRunningTaskMetadata[Nothing] =   SSRunningTaskMetadata[Nothing]("","",LocalDateTime.of(1970,1,1,0,0,0))
}

final case class SSRunningTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime) extends SSTaskMetaData {
 override def elapsedTime(): JDuration = JDuration.between(startedAt, LocalDateTime.now)
}

final case class SSCompletedTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime, completedAt: LocalDateTime, t: T) extends SSTaskMetaData {
 override def elapsedTime(): JDuration = JDuration.between(startedAt, completedAt)
}

final case class SSFailedTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime, completedAt: LocalDateTime, thr: Throwable) extends SSTaskMetaData {
 override def elapsedTime(): JDuration = JDuration.ZERO
}

final case class SSAbortedTaskMetadata[+T](id: String, name: String, abortedAt: LocalDateTime) extends SSTaskMetaData {
 override def elapsedTime(): JDuration = JDuration.ZERO
}
