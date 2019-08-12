package com.fijimf.deepfij.models

import java.io.InputStream

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleSerializerSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import testhelpers.Injector

import scala.concurrent.Await
import scala.io.Source

class ConferenceStandingsSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  type SB = com.fijimf.deepfij.models.nstats.Scoreboard

  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  import com.fijimf.deepfij.schedule.services.ScheduleSerializer._

  val isS3Json: InputStream = classOf[ScheduleSerializerSpec].getResourceAsStream("/test-data/s3Sched.json")
  val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes


  "ConferenceStandings " should {
    "calculate standings " in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) =>
          Await.result(saveToDb(uni, dao, repo), testDbTimeout)
          val ss = Await.result(dao.loadSchedule(2018), testDbTimeout)
          ss.foreach(s => {
            println(s"${s.season.year} ${s.games.size}")
            s.conferenceStandings.values.foreach { case (c, cs) =>
              println(s"\n${c.name}")
              cs.records.foreach { case (cwl, ncwl, t) =>
                println("%-20s    %2d - %2d   %2d - %2d ".format(t.name, cwl.won, cwl.lost, ncwl.won, ncwl.lost))
              }
            }
          })
      }
    }

  }


  /*
Louisiana	16	–	2	 	.889	 	 	27	–	7	 	.794
Georgia State †	12	–	6	 	.667	 	 	24	–	11	 	.686
Georgia Southern	11	–	7	 	.611	 	 	21	–	12	 	.636
Texas–Arlington	10	–	8	 	.556	 	 	21	–	13	 	.618
Louisiana–Monroe	9	–	9	 	.500	 	 	16	–	16	 	.500
Troy	9	–	9	 	.500	 	 	16	–	17	 	.485
Appalachian State	9	–	9	 	.500	 	 	15	–	18	 	.455
Coastal Carolina	8	–	10	 	.444	 	 	14	–	18	 	.438
South Alabama	7	–	11	 	.389	 	 	14	–	18	 	.438
Texas State	7	–	11	 	.389	 	 	15	–	18	 	.455
Arkansas State	6	–	12	 	.333	 	 	11	–	21	 	.344
Little Rock	4	–	14	 	.222	 	 	7	–	25	 	.219
   */
}
