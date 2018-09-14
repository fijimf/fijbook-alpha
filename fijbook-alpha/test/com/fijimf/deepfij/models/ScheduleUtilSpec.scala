package com.fijimf.deepfij.models

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.dao.schedule.util.ScheduleUtil
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.test.WithApplication
import testhelpers.Injector

import scala.concurrent.Await

class ScheduleUtilSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  val teams = List(
    Team(0L, "albany-ny", "Albany (NY)", "", "", "America East Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "east-carolina", "East Carolina", "", "", "American Athletic Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "cincinnati", "Cincinnati", "", "", "American Athletic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "davidson", "Davidson", "", "", "Atlantic 10 Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "boston-college", "Boston College", "", "", "Atlantic Coast Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "fgcu", "FGCU", "", "", "Atlantic Sun Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "baylor", "Baylor", "", "", "Big 12 Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "butler", "Butler", "", "", "Big East Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "eastern-wash", "Eastern Wash.", "", "", "Big Sky Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "campbell", "Campbell", "", "", "Big South Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "illinois", "Illinois", "", "", "Big Ten Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "cal-poly", "Cal Poly", "", "", "Big West Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "elon", "Elon", "", "", "Colonial Athletic Association", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "col-of-charleston", "Col. of Charleston", "", "", "Colonial Athletic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "charlotte", "Charlotte", "", "", "Conference USA", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "cleveland-st", "Cleveland St.", "", "", "Horizon League", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "canisius", "Canisius", "", "", "Metro Atlantic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "akron", "Akron", "", "", "Mid-American Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "bethune-cookman", "Bethune-Cookman", "", "", "Mid-Eastern Athletic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "bradley", "Bradley", "", "", "Missouri Valley...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "air-force", "Air Force", "", "", "Mountain West Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "bryant", "Bryant", "", "", "Northeast Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "austin-peay", "Austin Peay", "", "", "Ohio Valley Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "arizona", "Arizona", "", "", "Pac-12 Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "american", "American", "", "", "Patriot League", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "alabama", "Alabama", "", "", "Southeastern Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "chattanooga", "Chattanooga", "", "", "Southern Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "am-corpus-chris", "A&M-Corpus Chris", "", "", "Southland Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "alabama-am", "Alabama A&M", "", "", "Southwestern Athletic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "appalachian-st", "Appalachian St.", "", "", "Sun Belt Conference", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "brown", "Brown", "", "", "The Ivy League", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "denver", "Denver", "", "", "The Summit League", None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "byu", "BYU", "", "", "West Coast Conference",  None, None, None, None, None, None, None, LocalDateTime.now(), "Test"),
    Team(0L, "bakersfield", "UC Bakersfield", "", "", "Western Athletic...", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
  )

  val confs = List(
    Conference(0L, "colonial-athletic", "Colonial Athletic", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "atlantic-coast", "Atlantic Coast Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "independents", "Independents", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-west", "Big West Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "southland", "Southland Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "conference-usa", "Conference USA", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "southeastern", "Southeastern Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-12", "Big 12 Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "america-east", "America East Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "mountain-west", "Mountain West Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "western-athletic", "Western Athletic", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-south", "Big South Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "southern", "Southern Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-sky", "Big Sky Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "mid-eastern", "Mid-Eastern Athletic", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "ohio-valley", "Ohio Valley Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-ten", "Big Ten Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "american-athletic-conference", "American Athletic Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "patriot-league", "Patriot League", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "mid-american", "Mid-American Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "sun-belt", "Sun Belt Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "horizon", "Horizon League", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "southwestern-athletic", "Southwestern Athletic", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "metro-atlantic-athletic", "Metro Atlantic", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "atlantic-10", "Atlantic 10 Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "pac-12", "Pac-12 Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "west-coast", "West Coast Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "northeast", "Northeast Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "summit-league", "The Summit League", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "ivy-league", "The Ivy League", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "atlantic-sun", "Atlantic Sun Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "missouri-valley", "Missouri Valley", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test"),
    Conference(0L, "big-east", "Big East Conference", "Unknown", None, None, None, None, None, LocalDateTime.now(), "test")
  )

  "ScheduleUtil " should {
    "create seed mapped conferences" in new WithApplication() {
      val s = Season(0L, 2017)
      val ss = Await.result(dao.saveSeason(s), testDbTimeout)
      val ts = Await.result(dao.saveTeams(teams), testDbTimeout)
      val cs = Await.result(dao.saveConferences(confs), testDbTimeout)
      assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
      private val maps: List[ConferenceMap] = Await.result(ScheduleUtil.createConferenceMapSeeds(dao, "test"), testDbTimeout)
      assert(maps.forall(_.seasonId==ss.id))
      assert(maps.forall(_.conferenceId>0L))
      assert(maps.size== ts.size)
      ts.foreach(t=>{
        val teamIds: List[Long] = maps.map(_.teamId)
        assert(teamIds.contains(t.id)) //Every team mapped
      })
    }
  }
}
