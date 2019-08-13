package com.fijimf.deepfij.schedule.services

final case class ScheduleUpdateResult(source: String, inserted: Seq[Long], updated: Seq[Long], deleted: Seq[Long], unchanged: Seq[Long]) {
  override def toString: String = s"  Inserted: ${inserted.size}  Updated: ${updated.size}  Deleted: ${deleted.size}  Unchanged: ${unchanged.size}."

  def merge(newTag: String, updateDbResult: ScheduleUpdateResult): ScheduleUpdateResult = {
    ScheduleUpdateResult(newTag, inserted ++ updateDbResult.inserted, updated ++ updateDbResult.updated, unchanged ++ updateDbResult.unchanged, deleted ++ updateDbResult.deleted)
  }
}
