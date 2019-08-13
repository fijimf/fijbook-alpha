package com.fijimf.deepfij.schedule.model

import java.time.LocalDate

import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update

case class Season(id: Long, year: Int){
  val startDate: LocalDate = Season.startDate(year)
  val endDate: LocalDate = Season.endDate(year)
  val dates: List[LocalDate] = Season.dates(year)

  def containsDate(d: LocalDate): Boolean = !(d.isBefore(startDate) || d.isAfter(endDate))
}

object Season {

  def startDate(y: Int): LocalDate = LocalDate.of(y - 1, 11, 1)

  def endDate(y: Int): LocalDate = LocalDate.of(y, 4, 30)

  def dates(y: Int): List[LocalDate] = scala.Stream.iterate(startDate(y)) {
    _.plusDays(1)
  }.takeWhile(_.isBefore(endDate(y))).toList


  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Season, Long] {

    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
      CREATE TABLE season (
        id BIGSERIAL PRIMARY KEY,
        year INT NOT NULL);
      CREATE UNIQUE INDEX ON season(year);
      """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
      DROP TABLE IF EXISTS  season;
      """.update.run

    val select=fr"""SELECT id, year FROM season"""
    val delete=fr"""DELETE FROM season"""

    def saveSeason(s: Season): doobie.ConnectionIO[Season] = {
      sql"""INSERT INTO season(year) values (${s.year}) RETURNING id, year"""
        .update
        .withUniqueGeneratedKeys[Season]("id", "year")
    }

    def saveSeasons(seasons: List[Season]): doobie.ConnectionIO[List[Season]] = {
      Update[Int]("""INSERT INTO season(year) values (?) RETURNING id, year""")
        .updateManyWithGeneratedKeys[Season]("id","year")(seasons.map(_.year))
        .compile
        .to[List]
    }

    def findSeasonById(id: Long): doobie.ConnectionIO[Option[Season]] = {
      sql"""SELECT * FROM season WHERE id = $id"""
        .query[Season]
        .option
    }

    def findSeasonByYear(year: Int): doobie.ConnectionIO[Option[Season]] = {
      sql"""SELECT * FROM season WHERE year = $year""".query[Season].option
    }

    def deleteSeason(id: Long): doobie.ConnectionIO[Int] = {
      sql"""DELETE FROM season where id = $id """.update.run
    }

    def listSeasons: doobie.ConnectionIO[List[Season]] = {
      sql"""SELECT * FROM season""".query[Season].to[List]
    }
  }

}