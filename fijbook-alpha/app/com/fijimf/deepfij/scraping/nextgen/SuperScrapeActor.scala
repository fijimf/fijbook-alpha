package com.fijimf.deepfij.scraping.nextgen

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.{ActorRef, FSM}

import scala.util.{Failure, Success}


/**
  * SuperScrapeActor takes a List of SuperScrapeTasks and executes them sequentially.
  * SuperScrapeActor has the following states
  * READY - the SuperScrapeActor is not currently processing tasks, and can accept new task lists.
  * When in the READY state the SuperScrapeActor can respond to the following messages:
  * Status - will respond with the status - ("Ready", timeOfLastTaskList, durationOfLastTaskList)
  * ProcessTasks(taskList) - Will initiate the processing of the TaskList
  * PROCESSING/AWAITING_RESPONSE - the SuperScrapeActor is currently processing a task list
  * When in the PROCESSING/AWAITING_RESPONSE state the SuperScrapeActor can respond to the following messages:
  * Status - will respond with the status This will include the current taskList metadata, the current task metadata
  * AttachListener - this will add an Actor to listen to status updates as the task process continues
  * CancelTask - this will cancel the current task.  This may result in the taskList being abandoned,depending on 
  * how it is set up
  * CancelTaskList - this will cancel the current task and any subsequent tasks
  * Response - this is the response from the downstream actor that the task has been completed
  */



// SuperScrapeActorMessages
case object SSStatus

case class SSAttachListener(ref: ActorRef)

case object SSCancelTask

case object SSCancelTaskList

case class SSSuccessResponse[T](id: String, t: T)

case class SSFailureResponse(id: String, thr: Throwable)

case class SSProcessTasks(tasks: List[SSTask[_]])

case class SSTaskProgress(progress:Option[Double],message:Option[String])

sealed trait SuperScrapeActorState

case object Ready extends SuperScrapeActorState

case object Processing extends SuperScrapeActorState


class SuperScrapeActor() extends FSM[SuperScrapeActorState, SuperScrapeActorData] {
  startWith(Ready, ReadyData(None))

  when(Ready) {
    case Event(SSStatus, r: ReadyData) => stay replying r
    case Event(SSProcessTasks(tasks), _) if tasks.isEmpty => stay
    case Event(SSProcessTasks(tasks), _) if tasks.nonEmpty => goto(Processing) using {
      runNextTask(ProcessingData(SSTaskMetaData.uninitializedRunningTask, List.empty[SSTaskMetaData], tasks, List(sender)))
    }
  }


  when(Processing) {
    case Event(SSStatus, p: ProcessingData) => stay replying p
    case Event(SSAttachListener(ref), p: ProcessingData) => stay using p.copy(listeners = ref :: p.listeners)
    case Event(SSCancelTask, p: ProcessingData) =>
      val p1 = failCurrentTask(p, TaskCancelledException)
      if (p1.tasksToRun.isEmpty) {
        goto(Ready) using ReadyData(Some(SSTaskListMetadata(UUID.randomUUID().toString, p1.completedTasks)))
      } else {
        goto(Processing) using runNextTask(p)
      }

    case Event(SSCancelTaskList, p: ProcessingData) =>
      val q = p.tasksToRun.foldLeft(
        failCurrentTask(p, TaskCancelledException)
      ) { case (pn: ProcessingData, value: SSTask[_]) =>
        val unstartedTask = SSFailedTaskMetadata(UUID.randomUUID().toString, value.name, LocalDateTime.now(), LocalDateTime.now(), TaskNotStartedException)
        pn.copy(completedTasks = unstartedTask :: pn.completedTasks)
      }.copy(runningTask = SSTaskMetaData.uninitializedRunningTask, tasksToRun = List.empty[SSTask[_]])
      goto(Ready) using ReadyData(Some(SSTaskListMetadata(UUID.randomUUID().toString, q.completedTasks)))
    case Event(SSSuccessResponse(id, t), p: ProcessingData) if id == p.runningTask.id =>
      val p1 = succeedCurrentTask(p, t)
      if (p1.tasksToRun.isEmpty) {
        goto(Ready) using ReadyData(Some(SSTaskListMetadata(UUID.randomUUID().toString, p1.completedTasks)))
      } else {
        goto(Processing) using runNextTask(p1)
      }
    case Event(SSFailureResponse(id, thr), p: ProcessingData) if id == p.runningTask.id =>
      val p1 = failCurrentTask(p, thr)
      if (p1.tasksToRun.isEmpty) {
        goto(Ready) using ReadyData(Some(SSTaskListMetadata(UUID.randomUUID().toString, p1.completedTasks)))
      } else {
        goto(Processing) using runNextTask(p)
      }

    case Event(SSTaskProgress(x,msg), p:ProcessingData) =>
      p.listeners.foreach(_ ! (p.runningTask.id,p.runningTask.elapsedTime(),x.getOrElse(-1.0),msg.getOrElse("")))      
      stay()
  }

  onTransition {
    case Ready -> Processing =>
      println("Transform: Ready->Processing")
      nextStateData match {
        case (p: ProcessingData) => p.listeners.foreach(_ ! p)
        case _ =>
      }
    case Processing -> Processing =>
      println("Transform: Processing->Processing")
      nextStateData match {
        case (p: ProcessingData) => p.listeners.foreach(_ ! p)
        case _ =>
      }
    case Processing -> Ready =>
      println("Transform: Processing->Ready")
      (stateData, nextStateData) match {
        case (p: ProcessingData, r: ReadyData) => p.listeners.foreach(_ ! r)
        case _ =>
      }
  }

  def succeedCurrentTask[T](p: ProcessingData, t: T): ProcessingData = {
    val x = p.runningTask
    p.copy(completedTasks = SSCompletedTaskMetadata(x.id, x.name, x.startedAt, LocalDateTime.now(), t) :: p.completedTasks)
  }

  def failCurrentTask(p: ProcessingData, thr: Throwable): ProcessingData = {
    val x = p.runningTask
    p.copy(completedTasks = SSFailedTaskMetadata(x.id, x.name, x.startedAt, LocalDateTime.now(), thr) :: p.completedTasks)
  }


  def runNextTask(p: ProcessingData): ProcessingData = {
    import scala.concurrent.ExecutionContext.Implicits.global
    require(p.tasksToRun.nonEmpty)
    p.tasksToRun match {
      case head :: tail =>
        val id = head.id
        head.run(Some(self)).onComplete {
          case Success(t) => self ! SSSuccessResponse(id, t)
          case Failure(thr) => self ! SSFailureResponse(id, thr)
        }
        p.copy(
          runningTask = SSRunningTaskMetadata(id, head.name, LocalDateTime.now()),
          tasksToRun = tail
        )
    }

  }
}

object TaskCancelledException extends RuntimeException

object TaskNotStartedException extends RuntimeException
