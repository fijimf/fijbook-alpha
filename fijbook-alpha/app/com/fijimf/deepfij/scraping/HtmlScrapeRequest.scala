package com.fijimf.deepfij.scraping


import scala.xml.Node

trait HtmlScrapeRequest[T] extends HttpScrapeRequest[T]{
  def scrape(n: Node): T
}