package com.fijimf.deepfij.scraping

import java.time.LocalDateTime

import com.fijimf.deepfij.models.Team

import scala.xml.Node

case class TeamDetail(key: String, shortName: String, user: String) extends HtmlScrapeRequest[Team] with NcaaComTeamScraper {
  override def url = "http://www.ncaa.com/schools/" + key + "/"

  override def scrape(n: Node) = {
    val longName = schoolName(n).getOrElse(shortName)
    val metaInfo = schoolMetaInfo(n)
    val nickname = metaInfo.getOrElse("nickname", "MISSING")
    val primaryColor = schoolPrimaryColor(n)
    val secondaryColor = primaryColor.map(c => desaturate(c, 0.4))
    val logoUrl = schoolLogo(n)
    val officialUrl = schoolOfficialWebsite(n)
    val officialTwitter = schoolOfficialTwitter(n)
    val officialFacebook = schoolOfficialFacebook(n)
    val conference = metaInfo.getOrElse("conf", "MISSING")

    Team(
      0,
      key,
      shortName,
      longName,
      nickname,
      conference,
      logoUrl,
      logoUrl.map(_.replace("40", "70")),
      primaryColor,
      secondaryColor,
      officialUrl,
      officialTwitter,
      officialFacebook,
      LocalDateTime.now(),
      user
    )
  }
}