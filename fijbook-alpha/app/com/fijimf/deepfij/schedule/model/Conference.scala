package com.fijimf.deepfij.schedule.model

import java.time.LocalDateTime

import cats.effect.Bracket
import com.fijimf.deepfij.model.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor


case class Conference
(
  id: Long,
  key: String,
  name: String,
  level:String = "Unknown",
  logoLgUrl: Option[String],
  logoSmUrl: Option[String],
  officialUrl: Option[String],
  officialTwitter: Option[String],
  officialFacebook: Option[String],
  updatedAt: LocalDateTime,
  updatedBy: String
){

}


object Conference {


  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Conference,Long]{
    private val update: doobie.Update0 =
      sql"""
         CREATE TABLE conference (
           id BIGSERIAL PRIMARY KEY,
           key VARCHAR(64) NOT NULL,
           name VARCHAR(64) NOT NULL,
           level VARCHAR(12)/*FIXME*/ NOT NULL,
           logo_lg_url VARCHAR(96) NULL,
           logo_sm_url VARCHAR(96) NULL,
           official_url VARCHAR(96) NULL,
           official_twitter VARCHAR(96) NULL,
           official_facebook VARCHAR(96) NULL,
           updated_at TIMESTAMP NOT NULL,
           updated_by VARCHAR(36) NOT NULL
           );
         CREATE UNIQUE INDEX ON conference(key);
         """.update
    val createDdl: doobie.ConnectionIO[Int] =
      update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  conference;
         """.update.run



    val select=fr"""SELECT id, key, name, level, logo_lg_url,logo_sm_url, official_url, official_twitter, official_facebook, updated_at, updated_by FROM conference"""
    val delete=fr"""DELETE FROM conference"""


  }

}