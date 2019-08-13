package com.fijimf.deepfij.scraping.model

trait CasablancaJsonScrapeRequest[T] extends HttpScrapeRequest[T] {

  def preProcessBody(s:String):String = s

  def scrape(js:String): T
}
