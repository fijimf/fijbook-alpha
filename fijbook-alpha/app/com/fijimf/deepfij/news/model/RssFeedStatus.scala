package com.fijimf.deepfij.news.model

import java.time.LocalDateTime

final case class RssFeedStatus(feed: RssFeed, lastSuccessfulUpdate: Option[LocalDateTime], lastPublishDate: Option[LocalDateTime], itemCount: Int, itemCountLastWeek: Int) {

}
