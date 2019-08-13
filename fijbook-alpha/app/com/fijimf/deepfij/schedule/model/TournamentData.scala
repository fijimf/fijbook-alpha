package com.fijimf.deepfij.schedule.model

import java.time.LocalDateTime

import com.fijimf.deepfij.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor

case class TournamentData
(
  id:Long,
  gameId:Long,
  tournamentKey:String,
  homeTeamSeed:Int,
  awayTeamSeed:Int,
  updatedAt:LocalDateTime,
  updatedBy:String
)

object TournamentData {


  case class Dao[M[_]](xa:Transactor[M]) extends ModelDao[TournamentData, Long] {
    val createDdl: doobie.ConnectionIO[Int] =sql"""
        CREATE TABLE tournament_data (
          id BIGSERIAL PRIMARY KEY,
          game_id BIGINT NOT NULL,
          tournament_key VARCHAR(36) NOT NULL,
          home_team_seed INT NOT NULL,
          away_team_seed INT NOT NULL,
          updated_at TIMESTAMP NOT NULL,
          updated_by VARCHAR(36) NOT NULL);
         CREATE UNIQUE INDEX ON tournament_data(game_id);
    """.update.run

    val dropDdl: doobie.ConnectionIO[Int] =sql"""
         DROP TABLE IF EXISTS  tournament_data
    """.update.run


    val select=fr"""SELECT id, game_id, tournament_key, home_team_seed, away_team_seed, updated_at, updated_by FROM tournament_data"""
    val delete=fr"""DELETE FROM tournament_data"""
  }
}
