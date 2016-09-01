package com.fijimf.deepfij.models.book

import com.fijimf.deepfij.models.Game


case class User(id: Long, name: String, balance: Int)

case class Bet(id: Long, homeBettor: User, awayBettor: User, game: Game, doubleSpread: Int, amount: Int)

case class Offer(id: Long, bettor: User, amount: Int)

case class Book(id: Long, game: Game, offers: List[Offer], bets: List[Bet])

sealed trait HomeAway

case object Home extends HomeAway

case object Away extends HomeAway