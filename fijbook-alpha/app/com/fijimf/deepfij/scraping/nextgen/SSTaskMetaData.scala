package com.fijimf.deepfij.scraping.nextgen

import java.time.LocalDateTime

sealed trait SSTaskMetaData

object SSTaskMetaData {
 val uninitializedRunningTask: SSRunningTaskMetadata[Nothing] =   SSRunningTaskMetadata[Nothing]("","",LocalDateTime.of(1970,1,1,0,0,0))
}

case class SSRunningTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime) extends SSTaskMetaData

case class SSCompletedTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime, completedAt: LocalDateTime, t: T) extends SSTaskMetaData

case class SSFailedTaskMetadata[+T](id: String, name: String, startedAt: LocalDateTime, completedAt: LocalDateTime, thr: Throwable) extends SSTaskMetaData

case class SSAbortedTaskMetadata[+T](id: String, name: String, abortedAt: LocalDateTime) extends SSTaskMetaData
