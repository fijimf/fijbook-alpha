package com.fijimf.deepfij.models.services

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{RssFeed, RssItem}
import com.google.inject.Inject
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

class RssFeedUpdateServiceImpl @Inject()(dao: ScheduleDAO, wsClient: WSClient)(implicit ec: ExecutionContext) extends RssFeedUpdateService {

  import scala.concurrent.ExecutionContext.Implicits._

  val logger = play.api.Logger(this.getClass)

  override def updateAllFeeds(): Future[List[RssItem]] = {
    dao.listRssFeeds().flatMap(lst => Future.sequence(lst.map(rf =>
      updateFeed(rf.id)
    ))).map(_.flatten)
  }

  override def updateFeed(id: Long): Future[List[RssItem]] = {
    dao.findRssFeedById(id).flatMap {
      case Some(feed) =>
        for {
          feedItems <- loadRssItems(feed)
          existingItems <- dao.findRssItemsByFeed(feed.id)
          savedItems <- dao.saveRssItems(removeDupes(feedItems, existingItems))
        } yield {
          feedItems
        }
      case None => Future(List.empty[RssItem])
    }
  }

  private def removeDupes(feed: List[RssItem], db: List[RssItem]): List[RssItem] = {
    val map = db.map(i => i.url -> i).toMap
    val newItems = feed.foldLeft(List.empty[RssItem]) { case (lst, item) =>
      map.get(item.url) match {
        case Some(fi) if item.publishTime.isAfter(fi.publishTime) =>
          logger.info(s"Updating ${fi.id} at ${fi.publishTime} with ${item.publishTime}")
          item.copy(id = fi.id) :: lst
        case Some(_) => lst
        case _ =>
          logger.info(s"Adding ${item.url}")
          item :: lst
      }
    }
    logger.info(s"Kept ${newItems.size} of ${feed.size}")
    newItems
  }

  private def loadRssItems(feed: RssFeed): Future[List[RssItem]] = {
    wsClient.url(feed.url).get().map(r => {
      Try {
        XML.loadString(r.body)
      } match {
        case Success(value) =>
          (for {
            channel <- (value \ "channel").headOption
          } yield {
            channelNodeToItems(feed.id, channel)
          }).getOrElse(List.empty[RssItem])

        case Failure(exception) =>
          List.empty[RssItem]
      }
    })
  }

  private def channelNodeToItems(id: Long, channel: Node): List[RssItem] = {
    (channel \\ "item").map(item => {
      for {
        title <- (item \ "title").headOption
        link <- (item \ "link").headOption
        desc <- (item \ "description").headOption
        pubDate <- (item \ "pubDate").headOption
        date <- parseDate(pubDate.text)
      } yield {
        RssItem(0L, id, title.text.trim, link.text.trim, None, date.toLocalDateTime, LocalDateTime.now())
      }
    }).toList.flatten
  }

  private def parseDate(str: String): Option[ZonedDateTime] = {
    val formatters = List(
      DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss zzz"), //Espn
      DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z"), //CBS
      DateTimeFormatter.ISO_ZONED_DATE_TIME
    )
    val ot = formatters.foldLeft(Option.empty[ZonedDateTime]) { case (o, f) =>
      o match {
        case Some(dt) => o
        case None => Try {
          ZonedDateTime.parse(str.trim, f)
        }.toOption
      }
    }
    if (ot.isEmpty) logger.warn(s"Failed to parse the following date: $str")
    ot
  }
}