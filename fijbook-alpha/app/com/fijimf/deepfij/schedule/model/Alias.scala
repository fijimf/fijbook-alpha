package com.fijimf.deepfij.schedule.model

import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.model.ModelDao
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update

case class Alias(id: Long, key: String, alias: String)


object Alias {


  case class Dao[M[_]](xa:Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[Alias, Long] {

    val createDdl: doobie.ConnectionIO[Int] =
      sql"""
         CREATE TABLE alias (id BIGSERIAL PRIMARY KEY, key VARCHAR(64) NOT NULL, alias VARCHAR(64) NOT NULL);
         CREATE UNIQUE INDEX ON alias(alias);
         CREATE INDEX ON alias(key);
         """.update.run
    val dropDdl: doobie.ConnectionIO[Int] =
      sql"""
         DROP TABLE IF EXISTS  alias;
         """.update.run

    val select=fr"""SELECT id, key, alias FROM alias"""
    val delete=fr"""DELETE FROM alias"""

    def saveAlias(alias: Alias): doobie.ConnectionIO[Alias] =
      sql"""
           INSERT INTO alias VALUES (${alias.key}, ${alias.alias})
              ON CONFLICT (alias) DO UPDATE SET key=${alias.key}
              RETURNING id, key, alias
           """.update.withUniqueGeneratedKeys[Alias]("id", "key", "alias")



    def saveAliases(aliases: List[Alias]): doobie.ConnectionIO[List[Alias]] = {
      val tuples: List[(String, String, String)] = aliases.map(a => (a.key, a.alias, a.key))
      Update[(String, String, String)]("""
           INSERT INTO alias VALUES (?, ?)
              ON CONFLICT (alias) DO UPDATE SET key=?
              RETURNING id, key, alias
           """).updateManyWithGeneratedKeys[Alias]("id", "key", "alias")(tuples).compile
        .to[List]
    }
  }

}
