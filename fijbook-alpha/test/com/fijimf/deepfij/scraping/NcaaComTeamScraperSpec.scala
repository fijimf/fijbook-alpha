package com.fijimf.deepfij.scraping

import org.scalatest.FlatSpec

import scala.io.Source

class NcaaComTeamScraperSpec extends FlatSpec {
  val isGeorgetown = classOf[NcaaComTeamScraperSpec].getResourceAsStream("/test-data/georgetown.html")
  private val georgetown = Source.fromInputStream(isGeorgetown)
  val isStatPage = classOf[NcaaComTeamScraperSpec].getResourceAsStream("/test-data/stat_page.html")
  private val statpage = Source.fromInputStream(isStatPage)
  val scraper = new NcaaComTeamScraper {}

  private val strGU: String = georgetown.mkString
  "NcaaComTeamScraper" should "parse a team's HTML in " in {
    HtmlUtil.loadHtmlFromString(strGU) match {
      case Some(n) => assert(n.nonEmpty)
      case None => fail("Failed to parse sample HTML")
    }
  }

  it should "extract the school logo from the team page" in {
    HtmlUtil.loadHtmlFromString(strGU).flatMap(n => scraper.schoolLogo(n)) match {
      case Some(logo) => assert(logo == "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/images/logos/schools/g/georgetown.40.png")
      case None => fail("Failed to identify logo")
    }
  }
  it should "extract the school name from the team page" in {
    HtmlUtil.loadHtmlFromString(strGU).flatMap(n => scraper.schoolName(n)) match {
      case Some(logo) => assert(logo == "Georgetown University")
      case None => fail("Failed to identify school name")
    }
  }

  it should "extract the school facebook from the team page" in {
    HtmlUtil.loadHtmlFromString(strGU).flatMap(n => scraper.schoolOfficialFacebook(n)) match {
      case Some(logo) => assert(logo == "http://www.facebook.com/WeAreGeorgetown")
      case None => fail("Failed to identify facebook")
    }
  }
  it should "extract the school twitter from the team page" in {
    HtmlUtil.loadHtmlFromString(strGU).flatMap(n => scraper.schoolOfficialTwitter(n)) match {
      case Some(logo) => assert(logo == "http://www.twitter.com/georgetownhoyas")
      case None => fail("Failed to identify twitter")
    }
  }
  it should "extract the school website from the team page" in {
    HtmlUtil.loadHtmlFromString(strGU).flatMap(n => scraper.schoolOfficialWebsite(n)) match {
      case Some(logo) => assert(logo == "http://guhoyas.com")
      case None => fail("Failed to identify website")
    }
  }

  "NcaaComTeamScraper" should "parse an NCAA team stats  HTML in " in {
    HtmlUtil.loadHtmlFromString(statpage.mkString) match {
      case Some(n) => assert(n.nonEmpty)
      case None => fail("Failed to parse sample HTML")
    }
  }

  it should "extract the school names and keys from a logo page" in {
    HtmlUtil.loadHtmlFromString(statpage.mkString).map(n => scraper.teamNamesFromStatPage(n)) match {
      case Some(iter) => iter.foreach(println(_))
      case None => fail("Failed to identify logo")
    }
  }

}