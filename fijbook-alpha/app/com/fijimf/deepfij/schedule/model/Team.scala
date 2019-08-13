package com.fijimf.deepfij.schedule.model

import java.time.LocalDateTime

import cats.effect.Bracket
import com.fijimf.deepfij.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor


case class Team
(
  id: Long,
  key: String,
  name: String,
  longName: String,
  nickname: String,
  optConference: String,
  logoLgUrl: Option[String],
  logoSmUrl: Option[String],
  primaryColor: Option[String],
  secondaryColor: Option[String],
  officialUrl: Option[String],
  officialTwitter: Option[String],
  officialFacebook: Option[String],
  updatedAt: LocalDateTime,
  updatedBy: String
) {

}

object Team {


  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Team, Long] {
    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE team (
           id BIGSERIAL PRIMARY KEY,
           key VARCHAR(64) NOT NULL,
           name VARCHAR(64) NOT NULL,
           long_name VARCHAR(72) NOT NULL,
           nickname VARCHAR(64) NOT NULL,
           conf_key VARCHAR(36) NOT NULL,
           logo_lg_url VARCHAR(96) NULL,
           logo_sm_url VARCHAR(96) NULL,
           primary_color VARCHAR(18) NULL,
           secondary_color VARCHAR(18) NULL,
           official_url VARCHAR(96) NULL,
           official_twitter VARCHAR(96) NULL,
           official_facebook VARCHAR(96) NULL,
           updated_at TIMESTAMP NOT NULL,
           updated_by VARCHAR(36) NOT NULL);
         CREATE UNIQUE INDEX ON team(key);
         CREATE UNIQUE INDEX ON team(name);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  team;
         """.update.run


    val select=fr"""SELECT id, key, name, long_name, nickname, conf_key, logo_lg_url,logo_sm_url, primary_color, secondary_color, official_url, official_twitter, official_facebook, updated_at, updated_by FROM team"""
    val delete=fr"""DELETE FROM team"""
  }

}
