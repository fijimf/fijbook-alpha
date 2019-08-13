package com.fijimf.deepfij.auth.services

import cats.effect.Bracket
import com.fijimf.deepfij.ModelDao
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import doobie.implicits._
import doobie.util.fragment
import doobie.util.transactor.Transactor

object PasswordOps {

  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[PasswordInfo, Long] {
    override def createDdl: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE password_info (
           id BIGSERIAL PRIMARY KEY,
           hasher VARCHAR(36) NOT NULL,
           password VARCHAR(64) NOT NULL,
           salt VARCHAR(64) NULL,
           key VARCHAR(36) NOT NULL
         );
         CREATE UNIQUE INDEX ON password_info(key);
 """.update.run


    override def dropDdl: doobie.ConnectionIO[Int] =
      sql"""
        DROP TABLE IF EXISTS password_info
        """.update.run


    override def select: fragment.Fragment = fr"""SELECT hasher, password, salt FROM password_info """

    override def delete: fragment.Fragment = fr"""DELETE FROM password_info """


    def findByLoginInfo(l: LoginInfo): doobie.Query0[PasswordInfo] = {
      (select ++ loginInfoPredicate(l.providerKey)).query[PasswordInfo]
    }

    private def loginInfoPredicate(key: String) = {
      fr"""WHERE key=${key}"""
    }

    def insert(l:LoginInfo, p: PasswordInfo): doobie.Update0 = {
      sql"""
        INSERT INTO password_info(hasher, password, salt, key) values
        (
          ${p.hasher},${p.password},${p.salt},${l.providerKey}
        ) RETURNING hasher, password, salt
        """.update
    }


    def updateByLoginInfo(l:LoginInfo, p: PasswordInfo): doobie.Update0 = {
      (fr"""
        UPDATE password_info SET hasher=${p.hasher}, password=${p.password}, salt=${p.salt}
        """
        ++ loginInfoPredicate(l.providerKey) ++
        fr"""
        RETURNING id, hasher, password, salt, key
        """).update

    }

    def upsert(l:LoginInfo, p: PasswordInfo):doobie.Update0 = {
      sql"""
        INSERT INTO password_info(hasher, password, salt, key) values
        (
          ${p.hasher},${p.password},${p.salt},${l.providerKey}
        ) ON CONFLICT (key) DO UPDATE SET hasher=${p.hasher}, password=${p.password}, salt=${p.salt}
        RETURNING hasher, password, salt
        """.update
    }

    def delete(l: LoginInfo): doobie.Update0 = {
      (delete ++ loginInfoPredicate(l.providerKey)).update
    }
  }

}
