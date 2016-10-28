package com.fijimf.deepfij.models

import java.util.UUID
import javax.inject.Inject

import com.fijimf.deepfij.models.models.daos.UserDAO
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Give access to the user object using Slick
  */
class TeamDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val repo:ScheduleRepository) extends TeamDAO with DAOSlick {
  val log = Logger(getClass)
  import dbConfig.driver.api._



  override def find(key: String) = {
    val q: Query[repo.TeamsTable, Team, Seq] = repo.teams.filter(team => team.key === key)
   db.run(q.result.headOption)
  }

  override def find(id: Long) = {
    val q: Query[repo.TeamsTable, Team, Seq] = repo.teams.filter(team => team.id === id)
    db.run(q.result.headOption)
  }

   override def save(team: Team /*, isAutoUpdate:Boolean */):Future[Int] = {
     log.info("Saving team "+team)
     val ft: Future[Option[Team]] = find(team.key)
     val ot = Await.result(ft, Duration.Inf)
     //ft.flatMap(ot=>{
       val v:DBIO[Int]=ot match {
        case Some(t) => {
          log.info("Updating teamId="+t.id)
          repo.teams.insertOrUpdate(team.copy(id = t.id))
        }
        case None => {
          log.info("Inserting")
          repo.teams.insertOrUpdate(team)
        }
      }
       db.run(v.transactionally).onComplete {
         case Success(i)=> log.info("save succeeded")

         case Failure(thr)=> log.error("save failed", thr)

       }
       Future(1)
     //})
  }

  override def list: Future[List[Team]] = {
    db.run(repo.teams.to[List].result)
  }


}