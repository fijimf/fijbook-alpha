package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Alias

import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait AliasDAO {

  def deleteAliases(): Future[Int]

  def saveAlias(a: Alias): Future[Int]

  def findAliasById(id: Long): Future[Option[Alias]]

  def listAliases: Future[List[Alias]]

  def deleteAlias(id: Long): Future[Int]
}
