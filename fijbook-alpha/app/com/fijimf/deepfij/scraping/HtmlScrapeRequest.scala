package com.fijimf.deepfij.scraping


import scala.xml.Node

trait HtmlScrapeRequest[T] {
  def url: String

  def scrape(n: Node): T
}