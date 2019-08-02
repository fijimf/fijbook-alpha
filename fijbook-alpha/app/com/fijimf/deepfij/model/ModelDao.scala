package com.fijimf.deepfij.model

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}

import doobie.implicits._
import doobie.util.{Get, Put, Read, Write}
import doobie.util.fragment.Fragment

object ModelDao {

  implicit val ldtRead: Read[LocalDateTime] = Read[Timestamp].map(ts => ts.toLocalDateTime)

  implicit val ldtWrite: Write[LocalDateTime] = Write[Timestamp].contramap(ldt =>Timestamp.valueOf(ldt))

  implicit val ldRead: Read[LocalDate] = Read[Date].map(dt => dt.toLocalDate)

  implicit val ldWrite: Write[LocalDate] = Write[Date].contramap(ld =>Date.valueOf(ld))

}
trait ModelDao[K, ID] {

  def createDdl: doobie.ConnectionIO[Int]

  def dropDdl: doobie.ConnectionIO[Int]

  def select: Fragment

  def delete: Fragment

  def idPredicate(id: ID)(implicit ID:Put[ID]): Fragment = fr"""WHERE id=$id"""

  def list(implicit K:Get[K]): doobie.ConnectionIO[List[K]] = select.query[K].to[List]

  def findById(id: ID)(implicit ID:Put[ID], K:Read[K]): doobie.ConnectionIO[Option[K]] = (select ++ idPredicate(id)).query[K].option

  def deleteById(id: ID)(implicit ID:Put[ID]): doobie.ConnectionIO[Int] = (delete ++ idPredicate(id)).update.run

}