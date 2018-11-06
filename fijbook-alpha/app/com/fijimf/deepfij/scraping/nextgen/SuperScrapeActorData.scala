package com.fijimf.deepfij.scraping.nextgen

import akka.actor.ActorRef

sealed trait SuperScrapeActorData

final case class ReadyData(last: Option[SSTaskListMetadata]) extends SuperScrapeActorData

final case class ProcessingData
(
  runningTask: SSRunningTaskMetadata[_],
  completedTasks: List[SSTaskMetaData],
  tasksToRun: List[SSTask[_]],
  listeners: List[ActorRef]
) extends SuperScrapeActorData
