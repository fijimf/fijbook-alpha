package com.fijimf.deepfij.models.book

import java.time.{Duration, LocalDateTime}

import scala.util.{Failure, Success, Try}

final case class Book
(
  state: BookState,
  game: Game,
  offers: List[Offer],
  bets: List[Bet]
) {
  def openOrClose(time:LocalDateTime, openWindow:Duration, closeWindow:Duration):Try[Book] = {
    val b = if (time.plus(closeWindow).isAfter(game.date)) {
      if (state==Closed){
        this
      } else {
        copy(state = Closed)
      }
    }else if (time.plus(openWindow).isAfter(game.date)) {
      if (state==Open){
        this
      }else {
        copy(state=Open)
      }
    } else {
      if (state==Closed){
        this
      } else {
        copy(state = Closed)
      }
    }
    if (b.state == Open && b.game.result.isDefined) {
      Failure(new IllegalStateException("Book cannot be open when Game has results "))
    } else {
      Success(b)
    }
  }

  def openOrClose(app:App):Try[Book] = {
    openOrClose(app.time, app.openWindow, app.closeWindow)
  }

  def open: Either[IllegalStateException, Book] = {
    if (state == Open) {
      Left(new IllegalStateException("Book is already open"))
    } else if (game.result.isDefined) {
      Left(new IllegalStateException("Game is completed no further betting"))
    } else {
      Right(copy(state = Open))
    }
  }

  def close: Either[IllegalStateException, Book] = {
    if (state == Closed) {
      Left(new IllegalStateException("Book is already closed"))
    } else {
      Right(copy(state = Closed))
    }
  }

  def offer(id: Long, bettorId: Long, side: Side, spreadTimes2: Int, amount: Int): Book = {
    require(amount > 0)
    Offer(id, bettorId, side, spreadTimes2, amount) :: offers
    matchOffers()
  }

  def matchOffers(): Book = {
    offers.filter(_.side == Home)
    val (h, a) = offers.partition(_.side == Home)
    h.sortBy(_.spreadTimes2)
    this
  }
}
