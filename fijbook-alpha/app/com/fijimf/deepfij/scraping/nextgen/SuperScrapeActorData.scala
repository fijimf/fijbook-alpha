package com.fijimf.deepfij.scraping.nextgen

import akka.actor.ActorRef

sealed trait SuperScrapeActorData

case class ReadyData(last: Option[SSTaskListMetadata]) extends SuperScrapeActorData

case class ProcessingData
(
  runningTask: SSRunningTaskMetadata[_],
  completedTasks: List[SSTaskMetaData],
  tasksToRun: List[SSTask[_]],
  listeners: List[ActorRef]
) extends SuperScrapeActorData
