package com.fijimf.deepfij.models.nstats

import com.fijimf.deepfij.models.Schedule
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO



class StatsWrapper(dao:ScheduleDAO) {

  def writeSnapshot(a:Analysis[_], s:Schedule, obs:Map[Long, Double] ): Unit = {

  }

  def updateStats(data:Map[Long,Double], s:Schedule) = {

    for (a<-List(Regression.ols))
    Analysis.analyzeSchedule(s, a, writeSnapshot(a,s,_))

  }





}
