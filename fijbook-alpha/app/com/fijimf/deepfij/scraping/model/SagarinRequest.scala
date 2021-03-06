package com.fijimf.deepfij.scraping.model

import scala.xml.Node

final case class SagarinRequest(year: Int) extends HtmlScrapeRequest[List[SagarinRow]] with SagarinScraper {
  override def url = s"https://www.usatoday.com/sports/ncaab/sagarin/$year/team/"

  override def scrape(n: Node) = {
    results(n, year).getOrElse(List.empty[SagarinRow])
  }
}
