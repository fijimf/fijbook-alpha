package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.RssItem
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class RssFeedUpdateServiceImpl @Inject()(dao: ScheduleDAO, wsClient:WSClient)(implicit ec: ExecutionContext) extends RssFeedUpdateService  {
  override def updateFeed(id: Long): Future[List[RssItem]] = {
    dao.findRssFeedById(id).flatMap {
      case Some(feed) =>
//        dao.findRssItemsByFeed(feed.id).flatMap(items=>{
//          val fr: Future[WSResponse] = wsClient.url(feed.url).get()
//          fr.onComplete() match {
//            case Success(response:WSResponse)=> response.body.getBytes
//            case Failure(ex)=>
//          }
//
//        })
        Future(List.empty[RssItem])
      case None => Future(List.empty[RssItem])
    }
  }

}