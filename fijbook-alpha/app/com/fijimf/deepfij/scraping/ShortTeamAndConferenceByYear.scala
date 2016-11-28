package com.fijimf.deepfij.scraping

import scala.xml.Node

case class ShortTeamAndConferenceByYear(y: Int) extends HtmlScrapeRequest[TeamConfMap] with NcaaOrgTeamScraper {
  override def url = "http://stats.ncaa.org/team/inst_team_list?academic_year=" + y + "&conf_id=-1&division=1&sport_code=MBB"

  override def scrape(n: Node) = TeamConfMap(extractConferenceMap(n), extractTeamMap(n))
}

case class TeamConfMap(confKey:  Map[Int, String], teamKey:  Map[Int, String]){
  println("Here's the fucking team list:"+teamKey)
}
