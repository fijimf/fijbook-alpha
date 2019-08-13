package com.fijimf.deepfij.scraping.model

import play.api.libs.json.JsValue

trait JsonScrapeRequest[T] extends HttpScrapeRequest[T] {

  def preProcessBody(s:String):String = s

  def scrape(js:JsValue): T
}
