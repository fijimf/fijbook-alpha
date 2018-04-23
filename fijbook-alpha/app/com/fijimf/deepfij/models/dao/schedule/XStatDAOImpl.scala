package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait XStatDAOImpl extends XStatDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def createXStatsFromSparkDump: Future[Boolean] = {
    db.run(
      DBIO.sequence(
        Seq(
          sqlu"alter table `_xstat` change `stat` `stat` varchar(255) not null;",
          sqlu"alter table `_xstat` change `team` `team` varchar(255) not null;",
          sqlu"create unique index `stat_value_idx1` on `_xstat` (`season`,`date`,`stat`,`team`);",
          sqlu"create index `stat_value_idx2` on `_xstat` (`season`,`date`,`stat`);",
          sqlu"create index `stat_value_idx3` on `_xstat` (`season`,`stat`,`team`);",
          sqlu"create index `stat_value_idx4` on `_xstat` (`season`,`date`,`team`);"
        )
      )
    ).flatMap(ns=> {
      db.run(
        DBIO.sequence(
          Seq(
            sqlu"drop table `xstat`;",
            sqlu"rename table `_xstat` to `xstat`;"
          )
        ).transactionally
      ).map(_ ++ ns)
    }).map(si=>si.foldLeft(true){case (b: Boolean, i: Int) => b && i>0})   
     
      
   
  }

  override def listStatValues: Future[List[XStat]] = db.run(repo.xstats.to[List].result)

  //  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.xstats.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)
  //
  //  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)
  //
  //  override def loadStatValues(modelKey: String, from:LocalDate, to:LocalDate): Future[List[StatValue]] = {
  //    db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.date>=from && sv.date<=to).to[List].result)
  //  }

}
