package com.fijimf.deepfij.models

import java.time.LocalDateTime

case class RSSFeedStatus(feed: RssFeed, lastSuccessfulUpdate: Option[LocalDateTime], lastPublishDate: Option[LocalDateTime], itemCount: Int, itemCountLastWeek: Int) {

}
