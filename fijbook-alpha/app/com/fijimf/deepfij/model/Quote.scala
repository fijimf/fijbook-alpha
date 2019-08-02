package com.fijimf.deepfij.model

import cats.effect.Bracket
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor


case class Quote(id: Long, quote: String, source: Option[String], url: Option[String], key: Option[String])

object Quote {
  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Quote, Long] {
    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE quote (
           id BIGSERIAL PRIMARY KEY,
           quote VARCHAR(256) NOT NULL,
           source VARCHAR(128) NOT NULL,
           url VARCHAR(128) NOT NULL,
           key VARCHAR(64) NULL);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS quote;
         """.update.run


    val select=fr"""SELECT id, quote, source, url, key FROM quote"""
    val delete=fr"""DELETE FROM quote"""
  }

}