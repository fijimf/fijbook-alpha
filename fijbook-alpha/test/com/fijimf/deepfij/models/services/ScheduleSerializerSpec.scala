package com.fijimf.deepfij.models.services

import java.time.LocalDateTime

import com.fijimf.deepfij.models.{Alias, Quote, Team}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.libs.json.Json
import play.api.test.WithApplication

import scala.concurrent.Await
import scala.util.{Failure, Success}

class ScheduleSerializerSpec extends PlaySpec with OneAppPerTest {
  val now = LocalDateTime.now()

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

  }
}