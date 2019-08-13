package com.fijimf.deepfij.templates

import java.io.InputStream
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{RebuildDatabaseMixin, Schedule, Team}
import com.fijimf.deepfij.schedule.model.Schedule
import com.fijimf.deepfij.schedule.services.ScheduleSerializer
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.libs.json.Json
import play.test.Helpers._
import play.twirl.api.HtmlFormat
import testhelpers.Injector

import scala.concurrent.Await
import scala.io.Source

class BlocksTest extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  import ScheduleSerializer._

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
    val t: Team = Team(1L, "georgetown", "Georgetown", "Georgetown University", "Hoyas", "", None, None, None, None, None, None, None, LocalDateTime.now, "")

    "provide a link to a team name" in {
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nameLink(t)
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="" >Georgetown</a>""")
    }

    "provide a link to a team name with a class" in {
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nameLink(t, Some("menu-item"))
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="menu-item" >Georgetown</a>""")
    }
  }

  "A nicknameLink" should {
    val t: Team = Team(1L, "georgetown", "Georgetown", "Georgetown University", "Hoyas", "", None, None, None, None, None, None, None, LocalDateTime.now, "")

    "provide a link to a team name with nickname" in {
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nicknameLink(t)
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="" >Georgetown Hoyas</a>""")
    }

    "provide a link to a team name with nickname with a class" in {
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.nameLink(t, Some("menu-item"))
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="menu-item" >Georgetown</a>""")
    }

  }

  "A nameLinkId" should {
    "provide a link to a team name by id" in  {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNameLink(219L)
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="" >Georgetown</a>""")
    }
    "provide a link to a team name by id with class" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNameLink(219L, Some("menu-item"))
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="menu-item" >Georgetown</a>""")
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
      assert(html.contentType === "text/html", "Correct content")
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="" >Georgetown Hoyas</a>""")
    }
    "provide a link to a team name with nickname with class by id" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNicknameLink(219L, Some("menu-item"))
      assert(contentAsString(html).trim ==="""<a href="/deepfij/teams/georgetown" class="menu-item" >Georgetown Hoyas</a>""")
    }

    "provide a blank string on an unknown id" in {
      implicit val s: Schedule = schedules.head
      val html: HtmlFormat.Appendable = views.html.data.blocks.team.idNicknameLink(-1L)
      assert(contentAsString(html).trim().isEmpty)
    }

  }

  "A dateLink" should {
    "format a date and provide a link" in {
      val d = LocalDate.of(2019, 1, 2)
      val html = views.html.data.blocks.date.dateLink(d, "MMMM d, yyyy")
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/20190102\""))
      assert(contentAsString(html).contains("January 2, 2019"))
      assert(!contentAsString(html).contains("{"), "No extra braces")
      assert(!contentAsString(html).contains("}"), "No extra braces")
    }

    "format a date with no link" in {
      val d = LocalDate.of(2019, 1, 2)
      val html = views.html.data.blocks.date.dateLink(d, "dd-MMM-yyyy", link = false)
      assert(html.contentType === "text/html")
      assert(!contentAsString(html).contains("href"))
      assert(contentAsString(html).contains("02-Jan-2019"))
      assert(!contentAsString(html).contains("{"), "No extra braces")
      assert(!contentAsString(html).contains("}"), "No extra braces")
    }
  }
  "A dateTimeLink" should {
    "format a date and provide a link" in {
      val d = LocalDateTime.of(2019, 1, 2, 13,30,15)
      val html = views.html.data.blocks.date.dateTimeLink(d, "MMMM d, yyyy h:mm")
      assert(html.contentType === "text/html")
      assert(contentAsString(html).contains("href=\"/deepfij/20190102\""))
      assert(contentAsString(html).contains("January 2, 2019 1:30"))
      assert(!contentAsString(html).contains("{"), "No extra braces")
      assert(!contentAsString(html).contains("}"), "No extra braces")
    }

    "format a date with no link" in {
      val d = LocalDateTime.of(2019, 1, 2, 13,30,15)
      val html = views.html.data.blocks.date.dateTimeLink(d, "dd-MMM-yyyy HH:mm:ss", link = false)
      assert(html.contentType === "text/html")
      assert(!contentAsString(html).contains("href"))
      assert(contentAsString(html).contains("02-Jan-2019 13:30:15"))
      assert(!contentAsString(html).contains("{"), "No extra braces")
      assert(!contentAsString(html).contains("}"), "No extra braces")
    }
  }

  " A game line" should {
    "correctly format a schedule line" in {
      implicit val s: Schedule = schedules.head
      s.teams.foreach(t => {
        s.games(t).foreach { case (g, or) =>
          val html = views.html.data.blocks.schedule.gameLine(t, g, or)
          assert(html.contentType === "text/html")
          val opp = if (g.homeTeamId == t.id) {
            s.teamsMap.get(g.awayTeamId)
          } else {
            s.teamsMap.get(g.homeTeamId)
          }
          assert(contentAsString(html).contains(s"""${DateTimeFormatter.ofPattern("MMM-dd-yyyy").format(g.date)}"""))
          opp.foreach(o =>
            if (!o.name.contains("&"))assert(contentAsString(html).contains(s"""<a href="/deepfij/teams/${o.key}" class="" >""")))
          assert(contentAsString(html).contains("<td>@</td>") || contentAsString(html).contains("<td>vs.</td>"))
          or.foreach{
            res=>assert(contentAsString(html).contains(s"<td>${res.homeScore} - ${res.awayScore}</td>"))
          }
          assert(!contentAsString(html).contains("{") && !contentAsString(html).contains("}"))

        }

      })

    }
  }




}
