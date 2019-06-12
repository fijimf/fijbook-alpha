package com.fijimf.deepfij.models.nstats.predictors

import java.io.InputStream

import akka.actor.ActorSystem
import com.fijimf.deepfij.models.RebuildDatabaseMixin
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats._
import com.fijimf.deepfij.models.services.{ScheduleSerializer, ScheduleSerializerSpec}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.libs.json.Json
import testhelpers.Injector

import scala.concurrent.{Await, Future}
import scala.io.Source

class StatFeatureExtractorSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]
  val actorSystem: ActorSystem = Injector.inject[ActorSystem]

  import ScheduleSerializer._

  val isS3Json: InputStream = classOf[ScheduleSerializerSpec].getResourceAsStream("/test-data/s3Sched.json")
  val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes

  val statsWrapper = StatsUpdater(dao, actorSystem)

  "The StatisticFeatureExtractor object " should {

    "generate features for a set of games using a Schedule and StatDao" in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) =>
          println("Loading schedule to db.")
          Await.result(saveToDb(uni, dao, repo), testDbTimeout)
          val ss = Await.result(dao.loadSchedules(), testDbTimeout)
         val sk= Await.result(Future.sequence(ss.map(s => {
            statsWrapper.updateStats(s, List(Counter.wins, Appender.meanMargin), testDbTimeout * 2)
          })), testDbTimeout * 2)
          assert(sk==="Snapshot buffer is done")
          println("OK")
          ss.foreach(s => {
            val extractor = StatisticFeatureExtractor( dao, List(("wins", "value")))
            val featureSets: List[(Long,Map[String, Double])] = Await.result(extractor(s.games), testDbTimeout)
            assert(featureSets.size === s.games.size)
            featureSets.foreach(fs => {
              assert(fs._2.size <= 2)
              assert(fs._2.size < 2 || fs._2.keySet.contains("wins.value.home"))
              assert(fs._2.size < 2 || fs._2.keySet.contains("wins.value.away"))
            })
            assert(featureSets.count(_._2.size === 2) > 0)
          })

          ss.foreach(s => {
            val extractor = StatisticFeatureExtractor(dao, List(("wins", "value"), ("wins", "rank"), ("mean-margin", "zscore")))
            val featureSets: List[(Long,Map[String, Double])] = Await.result(extractor(s.games), testDbTimeout)
            assert(featureSets.size === s.games.size)
            featureSets.foreach(fs => {
              assert(fs._2.size <= 6, s"Wrong number of keys ${fs._2.keySet.mkString(", ")}")
              assert(fs._2.size < 6 || fs._2.keySet.contains("wins.value.home"))
              assert(fs._2.size < 6 || fs._2.keySet.contains("wins.value.away"))
              assert(fs._2.size < 6 || fs._2.keySet.contains("wins.rank.home"))
              assert(fs._2.size < 6 || fs._2.keySet.contains("wins.rank.away"))
              assert(fs._2.size < 6 || fs._2.keySet.contains("mean-margin.zscore.home"))
              assert(fs._2.size < 6 || fs._2.keySet.contains("mean-margin.zscore.away"))
            })
            assert(featureSets.count(_._2.size === 6) > 0)
          })
        case None => fail()
      }
    }
  }
}
