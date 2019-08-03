package com.fijimf.deepfij.model.schedule

import java.time.LocalDateTime

import cats.effect.Bracket
import com.fijimf.deepfij.model.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor
case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long, updatedAt: LocalDateTime, updatedBy: String) {

}

object ConferenceMap {


  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[ConferenceMap, Long] {
    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE conference_map (
           id BIGSERIAL PRIMARY KEY,
           season_id BIGINT NOT NULL,
           conference_id BIGINT NOT NULL,
           team_id BIGINT NOT NULL,
           updated_at TIMESTAMP NOT NULL,
           updated_by VARCHAR(36) NOT NULL);
         CREATE UNIQUE INDEX ON conference_map(season_id, team_id);
         CREATE INDEX ON conference_map(season_id, conference_id);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  conference_map;
         """.update.run


    val select=fr"""SELECT id, season_id, conference_id, team_id, updated_at, updated_by FROM conference_map"""
    val delete=fr"""DELETE FROM conference_map"""
  }

}