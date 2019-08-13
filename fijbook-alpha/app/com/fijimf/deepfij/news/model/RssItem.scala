package com.fijimf.deepfij.news.model

import java.time.LocalDateTime

final case class RssItem(id: Long, rssFeedId: Long, title: String, url: String, image: Option[String], publishTime: LocalDateTime, recordedAt: LocalDateTime)
