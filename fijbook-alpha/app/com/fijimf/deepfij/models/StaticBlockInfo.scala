package com.fijimf.deepfij.models

import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Date, TimeZone}
import javax.servlet.jsp.tagext.TagData

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectTaggingRequest, GetObjectTaggingResult, Tag}

import scala.collection.JavaConversions._

case class StaticBlockInfo(key: String, lastModified: LocalDateTime, tags: Map[String, String])

object StaticBlockInfo {
  def list(s3: AmazonS3, bucket: String, folder: String): List[StaticBlockInfo] = {
    s3.listObjects(bucket).getObjectSummaries.map(os => {
      val key: String = os.getKey
      val lastModified: Date = os.getLastModified
      val taggingResult = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, key))
      val tags: Map[String, String] = taggingResult.getTagSet.foldLeft(Map.empty[String, String])((tagData: Map[String, String], tag: Tag) => {
        tagData + (tag.getKey -> tag.getValue)
      })
      StaticBlockInfo(key,LocalDateTime.ofEpochSecond(lastModified.getTime,0,ZoneOffset.UTC),tags)
    }).toList
  }
}
