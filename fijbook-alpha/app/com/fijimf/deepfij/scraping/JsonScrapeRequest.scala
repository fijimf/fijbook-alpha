package com.fijimf.deepfij.scraping


import play.api.libs.json.JsValue

trait JsonScrapeRequest[T] {
  def url: String

  def preProcessBody(s:String):String = s

  def scrape(js:JsValue): T
}