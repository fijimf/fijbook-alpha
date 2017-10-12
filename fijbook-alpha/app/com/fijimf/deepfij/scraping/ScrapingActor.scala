package com.fijimf.deepfij.scraping

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.Actor
import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class ScrapingResponse[T](url: String, latencyMs: Long, status: Int, length: Int, result: Try[T])

class ScrapingActor @Inject()(ws: WSClient) extends Actor {
  val logger: Logger = Logger(this.getClass)
  implicit val ec: ExecutionContext = new ExecutionContext {
    val threadPool: ExecutorService = Executors.newFixedThreadPool(4) // Limited so ncaa.com stops blocking me

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

  override def receive: Receive = {
    case r: HtmlScrapeRequest[_] => handleScrape(r)
    case r: JsonScrapeRequest[_] => handleJsonScrape(r)
    case TestUrl(url) => handleTest(url)
    case uxm =>
      logger.error(s"Unexpected message ${uxm.toString}")
  }

  def handle[T](r: HttpScrapeRequest[T], f: (String) => Try[T]): Unit = {
    val replyTo = sender()
    val start = System.currentTimeMillis()
    ws.url(r.url).get().onComplete {
      case Success(response) =>
        replyTo ! ScrapingResponse(r.url, System.currentTimeMillis() - start, response.status, response.body.length, f(response.body))
      case failure =>
        replyTo ! ScrapingResponse(r.url, System.currentTimeMillis() - start, -1, 0, failure)
    }
  }

  def handleScrape[T](r: HtmlScrapeRequest[T]): Unit = {
    handle(r, body => {
      for {
        node <- HtmlUtil.loadFromString(body)
        t <- Try {
          r.scrape(node)
        }
      } yield t
    })
  }

  def handleJsonScrape[T](r: JsonScrapeRequest[T]): Unit = {
    handle(r, body => {
      for {
        pr <- Try {
          r.preProcessBody(body)
        }
        js <- Try {
          Json.parse(pr)
        }
        t <- Try {
          r.scrape(js)
        }
      } yield t
    })
  }


  def handleTest(url: String): Unit = {
    logger.info("Received test req")
    val mySender = sender()
    logger.info("Requesting " + url)
    ws.url(url).get().onComplete {
      case Success(response) =>
        mySender ! Some(response.status)
      case Failure(ex) =>
        logger.warn(s"'GET $url' failed", ex)
        mySender ! None
    }
  }

}

case object EmptyBodyException extends RuntimeException
