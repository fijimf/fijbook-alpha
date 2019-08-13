package com.fijimf.deepfij.scraping.model

import scala.xml.Node

trait HtmlScrapeRequest[T] extends HttpScrapeRequest[T]{
  def scrape(n: Node): T
}
