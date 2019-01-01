package com.fijimf.deepfij.templates

import java.io.InputStream
import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{RebuildDatabaseMixin, Schedule, Season, Team}
import com.fijimf.deepfij.models.services.{ScheduleSerializer, ScheduleSerializerSpec}
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.libs.json.Json
import play.api.test.WithApplication
import play.test.Helpers._
import play.twirl.api.HtmlFormat
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class BlocksTest extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  import ScheduleSerializer._

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]
  val isS3Json: InputStream = classOf[BlocksTest].getResourceAsStream("/test-data/s3Sched.json")
  val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes

  def schedules: List[Schedule] = Json.parse(s3Sched).asOpt[MappedUniverse] match {
    case Some(uni) =>
      Await.result(saveToDb(uni, dao, repo), testDbTimeout)
      Await.result(dao.loadSchedules(), testDbTimeout)
    case _ => throw new RuntimeException
  }


  "A nameLink" should {
    "provide a link to a team name" in new WithApplication() {
      val t: Team = Team(1L, "georgetown", "Georgetown", "Georgetown University", "Hoyas", "", None, None, None, None, None, None, None, LocalDateTime.now, "")
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nameLink(t)
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/teams/georgetown\""))
      assert(contentAsString(html).contains("Georgetown"))
    }
  }

  "A nicknameLink" should {
    "provide a link to a team name with nickname" in new WithApplication() {
      val t: Team = Team(1L, "georgetown", "Georgetown", "Georgetown University", "Hoyas", "", None, None, None, None, None, None, None, LocalDateTime.now, "")
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nicknameLink(t)
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/teams/georgetown\""))
      assert(contentAsString(html).contains("Georgetown Hoyas"))
    }
  }

  "A nameLinkId" should {
    "provide a link to a team name by id" in  {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNameLink(219L)
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/teams/georgetown\""))
      assert(contentAsString(html).contains("Georgetown"))
    }

    "provide a blank string on an unknown id" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNameLink(-1L)
      assert(contentAsString(html).trim().isEmpty)
    }
  }

  "A nicknameLinkId" should {
    "provide a link to a team name with nickname by id" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNicknameLink(219L)
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/teams/georgetown\""))
      assert(contentAsString(html).contains("Georgetown Hoyas"))
    }

    "provide a blank string on an unknown id" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNicknameLink(-1L)
      assert(contentAsString(html).trim().isEmpty)
    }

  }




}
