package com.fijimf.deepfij.scraping

import play.api.libs.json.JsValue

trait CasablancaJsonScrapeRequest[T] extends HttpScrapeRequest[T] {

  def preProcessBody(s:String):String = s

  def scrape(js:String): T
}