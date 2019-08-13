package com.fijimf.deepfij.scraping.model

trait HttpScrapeRequest[T] {
  def url: String
}
