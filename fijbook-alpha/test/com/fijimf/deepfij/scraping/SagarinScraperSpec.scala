package com.fijimf.deepfij.scraping

import com.fijimf.deepfij.scraping.model.{HtmlUtil, SagarinRow, SagarinScraper}
import org.apache.commons.lang3.StringUtils
import org.scalatest.FlatSpec

import scala.io.Source

class SagarinScraperSpec extends FlatSpec {
  val isSagarin = classOf[SagarinScraperSpec].getResourceAsStream("/test-data/sagarin2017.html")
  private val sagarin = Source.fromInputStream(isSagarin)
  val scraper = new SagarinScraper {}

  private val strSag: String = sagarin.mkString
  "NcaaComTeamScraper" should "parse a USA Today Sagarin rating page HTML in " in {
    HtmlUtil.loadOptFromString(strSag) match {
      case Some(n) => assert(n.nonEmpty)
      case None => fail("Failed to parse sample HTML")
    }
  }

  it should "extract all the <pre> tags from the page" in {
    HtmlUtil.loadOptFromString(strSag).map(n => scraper.loadPres(n)) match {
      case Some(ps) =>
        assert(ps.size==4)
        assert(StringUtils.isBlank(ps(0).text))
        assert(ps(1).text.contains("Copyright"))
        assert(ps(2).text.contains(scraper.testTeam))
        assert(StringUtils.isBlank(ps(3).text))
      case None => fail("Failed to extract pres")
    }
  }

  it should "extract the <pre> tag with team data from the page" in {
    HtmlUtil.loadOptFromString(strSag).flatMap(n => scraper.loadTeamPre(n)) match {
      case Some(p) =>
        assert(p.text.contains(scraper.testTeam))
      case None => fail("Failed to extract teams pre")
    }
  }
  it should "extract the team candidate lines from the page" in {
    HtmlUtil.loadOptFromString(strSag).flatMap(n => scraper.breakupPre(n)) match {
      case Some(p) =>
        assert(p.size>351)
      case None => fail("Failed to extract teams pre")
    }
  }



  it should "extract the team candidate data from the page" in {
    HtmlUtil.loadOptFromString(strSag).flatMap(n => scraper.results(n,2017)) match {
      case Some(p) =>
        assert(p.size==351)
        p.zipWithIndex.forall((tuple: (SagarinRow, Int)) => tuple._1.rank-1==tuple._2)
      case None => fail("Failed to extract teams pre")
    }
  }
}