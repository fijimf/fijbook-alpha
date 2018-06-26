package com.fijimf.deepfij.models.book

import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}


///app
///app/bettor/add
///app/bettor/list
///app/bettor/n/show
///app/bettor/n/delete

// WHat is the lifecycle of this thing

//POST /app/create {name:"Name"} <= App page
//POST /app/bettor/create {name:"name"} <=App page
//GET  /app/{name} <=App page
//GET  /



object App {
  def create
  (
    name: String,
    time: LocalDateTime,
    openWindow: Duration = Duration.ofDays(7),
    closeWindow: Duration = Duration.ofHours(2)
  ): App = {
    App(name, time, openWindow, closeWindow)
  }
}

case class App
(
  name: String,
  time: LocalDateTime,
  openWindow: Duration,
  closeWindow: Duration,
  bettors: List[Bettor] = List.empty[Bettor],
  books: List[Book] = List.empty[Book]
) {


  private[this] val bettorMap: Map[String, Bettor] = bettors.map(b => b.name -> b).toMap
  private[this] val bookMap: Map[String, Book] = books.map(b => b.game.key -> b).toMap

  def addGame(homeTeam: String, awayTeam: String, date: LocalDateTime): Try[App] = {
    Try {
      Game(homeTeam, awayTeam, None, date)
    }.flatMap(g =>
      if (bookMap.exists{case (_,b)=>g.isSameGame(b.game)}) {
        Failure(new RuntimeException(s"Trying to add duplicate game with key '${g.key}"))
      } else {
        Book(Closed, g, List.empty[Offer], List.empty[Bet])
          .openOrClose(this)
          .map(b =>
            copy(books = b :: books, openWindow = Duration.ofDays(7), closeWindow = Duration.ofHours(2))
          )
      })
  }


  def addBettor(name: String, amount: Int = 0): Try[App] = {
    if (bettorMap.contains(name)) {
      Failure(new RuntimeException(s"Trying to add duplicate bettor with name '$name'"))
    } else {
      Success(copy(bettors = Bettor(name, amount) :: bettors, openWindow = Duration.ofDays(7), closeWindow = Duration.ofHours(2)))
    }
  }

  def synchronizeTime(newTime: LocalDateTime = LocalDateTime.now()): Try[App] = {
    val app = copy(time = newTime)
    Util.trySequence(books.map(_.openOrClose(app))) match {
      case Success(list) => Success(app.copy(books = list))
      case Failure(thr) => Failure(thr)
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

  def show: JsValue = Json.toJson(this)

}
