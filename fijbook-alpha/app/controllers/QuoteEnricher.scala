package controllers

import com.fijimf.deepfij.models.Quote
import com.fijimf.deepfij.models.react.{DisplayUser, QuoteWrapper}

import scala.concurrent.Future
import scala.util.Random

trait QuoteEnricher {
  self: WithDao =>

  import scala.concurrent.ExecutionContext.Implicits._

  def quoteKey: Option[String] = None

  val missing = Quote(-1L, " ", None, None, None)

  def getQuoteWrapper(du:DisplayUser):Future[QuoteWrapper] = {
    getQuote().map(q=>{
      if(du.isLoggedIn){
        if(du.dailyQuotesLiked.contains(q.id)){
          QuoteWrapper(q,isLiked = true,canVote = false)
        } else {
          QuoteWrapper(q,isLiked = false,canVote = true)
        }
      }else {
        QuoteWrapper(q,isLiked = false ,canVote = false)
      }

    })
  }


  def getQuote(): Future[Quote] =
    dao.listQuotes.map(allQuotes => {
      if (allQuotes.isEmpty) {
        missing
      } else {
        val unkeyedQuotes = allQuotes.filter(_.key.isEmpty)
        quoteKey match {
          case Some(k) =>
            val matchedQuotes = allQuotes.filter(_.key.contains(k))
            if (matchedQuotes.isEmpty || Random.nextDouble() > 0.5) {
              if (unkeyedQuotes.isEmpty) {
                missing
              } else {
                unkeyedQuotes(Random.nextInt(unkeyedQuotes.size))
              }
            } else {
              matchedQuotes(Random.nextInt(matchedQuotes.size))
            }
          case None => unkeyedQuotes(Random.nextInt(unkeyedQuotes.size))
        }
      }
    })
}
