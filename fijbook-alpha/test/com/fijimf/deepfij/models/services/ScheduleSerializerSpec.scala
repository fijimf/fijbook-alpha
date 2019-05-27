package com.fijimf.deepfij.models.services

import java.io.InputStream
import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Alias, Quote, RebuildDatabaseMixin, Team}
import org.apache.commons.lang3.StringUtils
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import testhelpers.Injector

import scala.concurrent.Await
import scala.io.Source

class ScheduleSerializerSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  val isS3Json: InputStream = classOf[ScheduleSerializerSpec].getResourceAsStream("/test-data/s3Sched.json")
  val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes

  val now: LocalDateTime = LocalDateTime.now()

  val baselineTeamTestData = Team(1L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "bigeast", Some("http://largeLogo.png"), Some("http://smallLogo.png"), Some("blue"), Some("gray"), Some("oifficialUrl"), Some("officialTwitter"), Some("officialFB"), now, "test")

  val listTeamTestData = List(
    (baselineTeamTestData, s"""{"id":1,"key":"georgetown","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","logoLgUrl":"http://largeLogo.png","logoSmUrl":"http://smallLogo.png","primaryColor":"blue","secondaryColor":"gray","officialUrl":"oifficialUrl","officialTwitter":"officialTwitter","officialFacebook":"officialFB","updatedAt":"$now","updatedBy":"test"}"""),
    (baselineTeamTestData.copy(id = 999),s"""{"id":999,"key":"georgetown","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","logoLgUrl":"http://largeLogo.png","logoSmUrl":"http://smallLogo.png","primaryColor":"blue","secondaryColor":"gray","officialUrl":"oifficialUrl","officialTwitter":"officialTwitter","officialFacebook":"officialFB","updatedAt":"$now","updatedBy":"test"}"""),
    (baselineTeamTestData.copy(key = "georgetown-u"),s"""{"id":1,"key":"georgetown-u","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","logoLgUrl":"http://largeLogo.png","logoSmUrl":"http://smallLogo.png","primaryColor":"blue","secondaryColor":"gray","officialUrl":"oifficialUrl","officialTwitter":"officialTwitter","officialFacebook":"officialFB","updatedAt":"$now","updatedBy":"test"}"""),
    (baselineTeamTestData.copy(logoLgUrl = None),s"""{"id":1,"key":"georgetown","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","logoSmUrl":"http://smallLogo.png","primaryColor":"blue","secondaryColor":"gray","officialUrl":"oifficialUrl","officialTwitter":"officialTwitter","officialFacebook":"officialFB","updatedAt":"$now","updatedBy":"test"}"""),
    (baselineTeamTestData.copy(logoSmUrl = None),s"""{"id":1,"key":"georgetown","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","logoLgUrl":"http://largeLogo.png","primaryColor":"blue","secondaryColor":"gray","officialUrl":"oifficialUrl","officialTwitter":"officialTwitter","officialFacebook":"officialFB","updatedAt":"$now","updatedBy":"test"}"""),
    (baselineTeamTestData.copy(logoLgUrl = None, logoSmUrl = None, primaryColor = None, secondaryColor = None, officialUrl = None, officialTwitter = None, officialFacebook = None),s"""{"id":1,"key":"georgetown","name":"Georgetown","longName":"Georgetown","nickname":"Hoyas","optConference":"bigeast","updatedAt":"$now","updatedBy":"test"}""")
  )

  val aliasTestData = Alias(23L, "alias", "key")
  val aliasJson ="""{"id":23,"alias":"alias","key":"key"}"""

  val baselineQuoteTestData = Quote(1L, "Quote", Some("Source"), None, None)
  val listQuoteTestData = List(
    (baselineQuoteTestData,s"""{"id":1,"quote":"Quote","source":"Source"}"""),
    (baselineQuoteTestData.copy(source = None),s"""{"id":1,"quote":"Quote"}"""),
    (baselineQuoteTestData.copy(url = Some("http://url")),s"""{"id":1,"quote":"Quote","source":"Source","url":"http://url"}"""),
    (baselineQuoteTestData.copy(key = Some("key")),s"""{"id":1,"quote":"Quote","source":"Source","key":"key"}""")

  )
  "ScheduleSerializer " should {
    import ScheduleSerializer._

    "format a Team to JSON" in {
      listTeamTestData.foreach {
        case (team: Team, json: String) => {
          assert(Json.toJson(team).toString === json)
        }
      }
    }

    "parse a Team from JSON" in {
      listTeamTestData.foreach {
        case (team: Team, json: String) => {
          assert(team === Json.parse(json).as[Team])
        }
      }

    }
    "format a Alias to JSON" in {
      assert(Json.toJson(aliasTestData).toString === aliasJson)
    }
    "parse a Alias from JSON" in {
      assert(aliasTestData === Json.parse(aliasJson).as[Alias])
    }

    "format a Quote to JSON" in {
      listQuoteTestData.foreach {
        case (quote: Quote, json: String) => {
          assert(Json.toJson(quote).toString === json)
        }
      }
    }

    "parse a Quote from JSON" in {
      listQuoteTestData.foreach {
        case (quote: Quote, json: String) => {
          assert(quote === Json.parse(json).as[Quote])
        }
      }
    }

    "parse a whole mapped universe bytes" in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) =>
          assert(uni.teams.size === 351)
          assert(uni.conferences.size === 32)
          assert(uni.aliases.size === 161)
          assert(uni.seasons.size === 5)

        case None =>

      }
    }

    "parse a whole mapped universe bytes and save it into db " in {
      import scala.concurrent.ExecutionContext.Implicits.global
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) =>
          Await.result(saveToDb(uni, dao, repo),testDbTimeout)
          assert(Await.result(dao.listTeams.map(_.size), testDbTimeout) === 351)
          assert(Await.result(dao.listConferences.map(_.size), testDbTimeout) === 32)
          assert(Await.result(dao.listAliases.map(_.size), testDbTimeout) === 161)
          assert(Await.result(dao.listSeasons.map(_.size), testDbTimeout) === 5)
        case None =>
      }
    }

    "generate a MD5 for a schedule " in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) => {
          Await.result(saveToDb(uni, dao, repo), testDbTimeout)
          val ss = Await.result(dao.loadSchedules, testDbTimeout)
          ss.foreach(s => {
            val md5 = ScheduleSerializer.md5Hash(s)
            assert(StringUtils.isNotBlank(md5))
            log.info(s.season.year + "->" + md5)
          })
          val ts = Await.result(dao.loadSchedules, testDbTimeout)
          ss.zip(ts).foreach{case (s,t)=>
            assert(ScheduleSerializer.md5Hash(s)===ScheduleSerializer.md5Hash(t))
          }
        }
        case None => fail("Failed to load test data")
      }
    }

    "generate MD5 hahses which track changes for a schedule " in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) => {
          Await.result(saveToDb(uni, dao, repo), testDbTimeout)
          val ss = Await.result(dao.loadSchedules, testDbTimeout)
          val s1 = ss.head
          ss.tail.foreach(s=>{
            assert(ScheduleSerializer.md5Hash(s1) != ScheduleSerializer.md5Hash(s))
          })

          val s1Prime = s1.copy(gameResults=s1.gameResults.drop(1))
          log.info(s1+" "+s1Prime)
          assert(ScheduleSerializer.md5Hash(s1) != ScheduleSerializer.md5Hash(s1Prime))
        }
        case None => fail("Failed to load test data")
      }
    }
  }
}