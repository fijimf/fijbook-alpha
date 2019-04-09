package com.fijimf.deepfij.models.nstats

import java.time.LocalDate
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern._
import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.actor.SnapshotBuffer
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{CalcStatus, Schedule, Season, Team, XStat}
import play.api.Logger

import scala.collection.immutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

final case class SnapshotDbBundle(d: LocalDate, k: String, xs: List[XStat])

final case class StatsUpdater(dao: ScheduleDAO, actorSystem: ActorSystem) {
  val logger = Logger(this.getClass)

  def writeSnapshot(a: Analysis[_], s: Schedule, key: String, snapshot: Snapshot): Unit = {
    if (snapshot.nonEmpty()) {
      val xstats = createTeamStatValues(a.key, s.season, s.teams, snapshot)
      logger.info(s"Write snapshot ${snapshot.n} ${snapshot.obs.size} ${xstats.size}")
      actorSystem.actorSelection(s"/user/$key") ! SnapshotDbBundle(snapshot.date, a.key, xstats)
    }
  }

  def createTeamStatValues(statKey: String, season: Season, teams: List[Team], snapshot: Snapshot): List[XStat] = {
    teams.map(t => {
      val id = t.id
      XStat(
        id = 0L,
        seasonId = season.id,
        date = snapshot.date,
        key = statKey,
        teamId = id,
        value = snapshot.value(id),
        rank = snapshot.rank(id),
        percentile = snapshot.percentile(id),
        mean = snapshot.mean,
        stdDev = snapshot.stdDev,
        min = snapshot.min,
        max = snapshot.max,
        n = snapshot.n
      )
    })
  }

  def updateStats(s: Schedule, models: List[Analysis[_]], timeout:FiniteDuration): Future[Any]= {

    import scala.concurrent.ExecutionContext.Implicits.global
    val instanceId = UUID.randomUUID().toString
    val key = s"snapshot-$instanceId"
    val promise:Promise[Any]=Promise[Any]
    val buffer = actorSystem.actorOf(Props(classOf[SnapshotBuffer], dao, promise), key)


    val schedHash = ScheduleSerializer.md5Hash(s)

    logger.info(s"Hash value for ${s.season.year} is $schedHash")

    val analyses: Future[List[Analysis[_]]] = selectModelsToBeUpdated(s.season, models, schedHash)

    analyses.map {
      case Nil =>
        logger.info("No models to run")
        sendCompletionMessage(timeout, key, buffer)
        //Complete
      case m :: Nil =>
        for {
          _ <- dao.deleteXStatBySeason(s.season, m.key)
          // Should have something to do the saving here
          _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m.key, schedHash))
        } yield {
          // Should return some kind of status
          Analysis.analyzeSchedule(s, m, writeSnapshot(m, s, key, _), terminateCallback(timeout, key, buffer))
        }
      case m :: ms =>
        ms.foreach(m1 => {
          for {
            _ <- dao.deleteXStatBySeason(s.season, m1.key)
            // Should have something to do the saving
            _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m1.key, schedHash))
          } yield {
            // Should return some kind of status
            Analysis.analyzeSchedule(s, m1, writeSnapshot(m1, s, key, _))
          }
        })
        for {
          _ <- dao.deleteXStatBySeason(s.season, m.key)
          _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m.key, schedHash))
        } yield {
          Analysis.analyzeSchedule(s, m, writeSnapshot(m, s, key, _), terminateCallback(timeout, key, buffer))
        }

    }
    promise.future
  }

  private def selectModelsToBeUpdated(season:Season, models: List[Analysis[_]], schedHash: String) = {
    Future.sequence(models.map(m => {
      modelNeedsUpdate(season, schedHash, m)
    })).map(_.flatten)
  }

  private def modelNeedsUpdate(season:Season, schedHash: String, m: Analysis[_]) = {

    dao.findStatus(season.id, m.key).map {
      case Some(c) if c.hash === schedHash =>
        logger.info(s"Skipping model ${m.key} for ${season.year}.  Stats are up to date")
        Option.empty[Analysis[_]]
      case _ => Some(m)
    }
  }

  private def sendCompletionMessage(timeout: FiniteDuration, key: String, buffer: ActorRef): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val completionMessage = buffer.ask(SendingComplete)(timeout)
    completionMessage.onComplete { msg =>
      logger.info(s"Processing complete for $key.  Sending kill to write buffer")
      logger.info(s"$msg")
      logger.info(s" Sending kill to write buffer")
      buffer ! PoisonPill
    }
  }

  private def terminateCallback(timeout: FiniteDuration, key: String, buffer: ActorRef): ((GameCalendar, _)) => Boolean = {
    tup => {
      val done = tup._1.date.isEmpty
      if (done) {
        sendCompletionMessage(timeout, key, buffer)
      }
      done
    }
  }

}
