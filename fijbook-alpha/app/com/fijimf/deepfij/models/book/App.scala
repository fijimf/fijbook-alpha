package com.fijimf.deepfij.models.book

import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter

import scala.util.{Failure, Success, Try}


object App {
  def create
  (
    name: String,
    time: LocalDateTime,
    openWindow:Duration = Duration.ofDays(7),
    closeWindow:Duration = Duration.ofHours(2)
  ): App = {
    App(name, time, openWindow , closeWindow , seq = seqNumGen())
  }

  def seqNumGen(): SequenceNumberGenerator = new SequenceNumberGenerator {
    private[this] var i = 0L

    override def next(): Long = {
      i = i + 1
      i
    }
  }
}

case class App
(
  name: String,
  time: LocalDateTime,
  openWindow: Duration,
  closeWindow: Duration,
  seq: SequenceNumberGenerator,
  bettors: List[Bettor] = List.empty[Bettor],
  books: List[Book] = List.empty[Book]
) {
  private def gameKey(g: Game): (String, Set[String]) = {
    (g.date.format(DateTimeFormatter.ISO_LOCAL_DATE), Set(g.homeTeam, g.awayTeam))
  }

  private[this] val bettorMap: Map[String, Bettor] = bettors.map(b => b.name -> b).toMap
  private[this] val bookMap: Map[(String, Set[String]), Book] = books.map(b => gameKey(b.game) -> b).toMap

  def addGame(homeTeam: String, awayTeam: String, date: LocalDateTime): Try[App] = {
    val g = Game(0L, homeTeam, awayTeam, None, date)
    val k = gameKey(g)
    if (k._2.size < 2) {
      Failure(new RuntimeException(s"Trying to add game with home team the same as away"))
    } else {
      if (bookMap.contains(k)) {
        Failure(new RuntimeException(s"Trying to add duplicate game with key '${k._1}, (${k._2.mkString(", ")})'"))
      } else {
        Book(0L, Closed, g, List.empty[Offer], List.empty[Bet])
          .openOrClose(this)
          .map(b=>
            copy(books = b :: books, openWindow = Duration.ofDays(7), closeWindow = Duration.ofHours(2))
          )
      }
    }
  }

  def addBettor(name: String, amount:Int=0): Try[App] = {
    if (bettorMap.contains(name)) {
      Failure(new RuntimeException(s"Trying to add duplicate bettor with name '$name'"))
    } else {
      Success(copy(bettors = Bettor(name, amount) :: bettors, openWindow = Duration.ofDays(7), closeWindow = Duration.ofHours(2)))
    }
  }

  def synchronizeTime(newTime: LocalDateTime = LocalDateTime.now()): Try[App] = {
    val app = copy(time=newTime)
    Util.trySequence(books.map(_.openOrClose(app))) match{
     case Success(list)=>Success(app.copy(books = list))
     case Failure(thr)=>Failure(thr)
   }
  }

  def incrementTime(x: Duration): Try[App] = {
    synchronizeTime(time.plus(x))
  }

  def deleteGame(): Try[App] = {
    Success(this)
  }

  def deleteBettor(): Try[App] = {
    Success(this)
  }

  def applyResult(gameId: Long, homeScore: Int, awayScore: Int): Try[App] = {
    Success(this)
  }

//  def offerBet(game:Game, side:Side, spreadTimes2:Int, amount:Int): Try[App] ={
//    bookMap.get(gameKey(game)) match {
//      case Some(book)=>{
//        book.addOffer(side, spreadTimes2, amount).map
//      }
//      case None=> Failure(new RuntimeException(s"Book not found for game ${gameKey(game)}"))
//    }
//  }
  def showBookDetail(gameId: Long): Book = ???

  def showBettorDetail(bettorId: Long): Bettor = ???

}
