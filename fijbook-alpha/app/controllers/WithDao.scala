package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

trait WithDao {
  def dao: ScheduleDAO
}
