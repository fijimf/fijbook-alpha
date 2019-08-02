package com.fijimf.deepfij.model

import java.time.{LocalDate, LocalDateTime}

import cats.effect.Bracket
import com.fijimf.deepfij.models.TournamentNode
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import doobie.implicits._

case class Game
(
  id: Long,
  seasonId: Long,
  homeTeamId: Long,
  awayTeamId: Long,
  date: LocalDate,
  datetime: LocalDateTime,
  location: Option[String],
  isNeutralSite: Boolean,
  sourceKey: String,
  updatedAt: LocalDateTime,
  updatedBy: String
) {


}

object Game {


  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Game, Long] {
    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE game (
           id BIGSERIAL PRIMARY KEY,
           season_id BIGINT NOT NULL,
           home_team_id BIGINT NOT NULL,
           away_team_id BIGINT NOT NULL,
           date DATE NOT NULL,
           datetime TIMESTAMP NOT NULL,
           location VARCHAR(48) NULL,
           is_neutral_site BOOLEAN NOT NULL,
           source_key VARCHAR(36) NOT NULL,
           updated_at TIMESTAMP NOT NULL,
           updated_by VARCHAR(36) NOT NULL);
         CREATE UNIQUE INDEX ON game(season_id, date, home_team_id, away_team_id);
         CREATE INDEX ON game(season_id, date);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  game;
         """.update.run

    val select=
      fr"""SELECT
             g.id, g.season_id, g.home_team_id, g.away_team_id, g.date, g.datetime, g.location, g.is_neutral_site, g.source_key, g.updated_at, g.updated_by
           FROM game g
           """
    val delete=fr"""DELETE FROM game"""
  }

}