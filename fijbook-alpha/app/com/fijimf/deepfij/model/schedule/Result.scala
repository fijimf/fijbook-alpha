package com.fijimf.deepfij.model.schedule

import java.time.LocalDateTime

import cats.effect.Bracket
import com.fijimf.deepfij.model.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor


case class Result
(
  id: Long,
  gameId: Long,
  homeScore: Int,
  awayScore: Int,
  periods: Int,
  updatedAt: LocalDateTime,
  updatedBy: String
) {

}

object Result {

  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Result, Long] {
    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE result (
           id BIGSERIAL PRIMARY KEY,
           game_id BIGINT NOT NULL,
           home_score INT NOT NULL,
           away_score INT NOT NULL,
           periods INT NOT NULL,
           updated_at TIMESTAMP NOT NULL,
           updated_by VARCHAR(36) NOT NULL);
         CREATE UNIQUE INDEX ON result(game_id);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  result;
         """.update.run


    val select=fr"""SELECT id, game_id, home_score, away_score, periods, updated_at, updated_by FROM result"""
    val delete=fr"""DELETE FROM result"""
  }

}
