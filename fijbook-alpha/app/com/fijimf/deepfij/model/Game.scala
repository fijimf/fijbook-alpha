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
  result:Option[Result],
  tournamentData: Option[TournamentData],
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
           source_ey VARCHAR(36) NOT NULL,
           updated_att TIMESTAMP NOT NULL,
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
             g.id, g.season_id, g.home_team_id, g.away_team_id, g.date, g.datetime, g.location, g.is_neutral_site
             r.id, r.game_id, r.home_score, r.away_score, r.periods, r.updated_at, r.updated_by,
             t.id, t.game_id, t.tournament_key, t.home_team_seed, t.away_team_seed, t.updated_at, t.updated_by,
             g.source_key, g.updated_at, g.updated_by
           FROM game g
           LEFT OUTER JOIN result r ON r.game_id=g.id
           LEFT OUTER JOIN tournament_data t ON t.game_id=g.id
           """
    val delete=fr"""DELETE FROM game"""
  }

}