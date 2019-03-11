package com.fijimf.deepfij.models.services

final case class UpdateDbResult(source: String, inserted: Seq[Long], updated: Seq[Long], deleted: Seq[Long], unchanged: Seq[Long]) {
  override def toString: String = s"  Inserted: ${inserted.size}  Updated: ${updated.size}  Deleted: ${deleted.size}  Unchanged: ${unchanged.size}."

  def merge(newTag: String, updateDbResult: UpdateDbResult): UpdateDbResult = {
    UpdateDbResult(newTag, inserted ++ updateDbResult.inserted, updated ++ updateDbResult.updated, unchanged ++ updateDbResult.unchanged, deleted ++ updateDbResult.deleted)
  }
}