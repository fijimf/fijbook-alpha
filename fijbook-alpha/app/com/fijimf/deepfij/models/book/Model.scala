package com.fijimf.deepfij.models.book

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}

import scala.util.{Failure, Success, Try}



case class Bettor
(
  name: String,
  balance: Int
) {
  def addBalance(amount: Int): Bettor = {
    copy(balance = balance + amount)
  }

  def subtractBalance(amount: Int): Bettor = {
    copy(balance = balance - amount)
  }
}

case class Bet
(
  id: Long,
  offerId: Long,
  homeBettorId: Long,
  awayBettorId: Long,
  spreadTimes2: Int,
  amount: Int
)

case class Offer
(
  id: Long,
  bettorId: Long,
  side: Side,
  spreadTimes2: Int,
  amount: Int
)


case class Game
(
  id: Long,
  homeTeam: String,
  awayTeam: String,
  result: Option[Result],
  date: LocalDateTime
)

case class Result
(
  homeScore: Int,
  awayScore: Int
)

sealed trait Side

case object Home extends Side

case object Away extends Side


sealed trait BookState

case object Open extends BookState

case object Closed extends BookState

trait SequenceNumberGenerator {
  def next(): Long
}