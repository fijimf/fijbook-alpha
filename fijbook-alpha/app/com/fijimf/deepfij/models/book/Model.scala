package com.fijimf.deepfij.models.book

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{Duration, LocalDateTime}

import scala.util.{Failure, Success, Try}


final case class Bettor
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

final case class Bet
(
  id: Long,
  offerId: Long,
  homeBettorId: Long,
  awayBettorId: Long,
  spreadTimes2: Int,
  amount: Int
)

final case class Offer
(
  id: Long,
  bettorId: Long,
  side: Side,
  spreadTimes2: Int,
  amount: Int
)


final case class Game
(
  homeTeam: String,
  awayTeam: String,
  result: Option[Result],
  date: LocalDateTime
) {
  require(homeTeam != awayTeam)
  val key = s"${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}-$homeTeam-$awayTeam"

  def isSameGame(g: Game): Boolean = g.date.truncatedTo(ChronoUnit.DAYS) == date.truncatedTo(ChronoUnit.DAYS) && Set(g.homeTeam, g.awayTeam) == Set(homeTeam, awayTeam)
}

final case class Result
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