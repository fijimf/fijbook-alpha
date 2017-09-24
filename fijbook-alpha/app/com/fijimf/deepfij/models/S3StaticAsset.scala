package com.fijimf.deepfij.models

import java.io.ByteArrayInputStream
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.amazonaws.util.IOUtils

import scala.collection.JavaConversions._

case class S3StaticAsset(key: String, relativeKey:String, lastModified: LocalDateTime, tags: Map[String, String])

object S3StaticAsset {

  val bucket ="fijimf-2017"
  val staticPageFolder="static-pages/"

  def list(s3: AmazonS3, bucket: String, folder: String): List[S3StaticAsset] = {
    s3.listObjects(bucket, folder).getObjectSummaries.map(os => {
      val key: String = os.getKey
      val lastModified: Date = os.getLastModified
      val tags: Map[String, String] = getTags(s3, bucket, key)
      S3StaticAsset(
        key,
        key.replace(folder,""),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified.getTime), ZoneOffset.UTC),
        tags
      )
    }).toList
  }

  private def getTags(s3: AmazonS3, bucket: String, key: String) = {
    val taggingResult = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, key))
    taggingResult.getTagSet.foldLeft(Map.empty[String, String])((tagData: Map[String, String], tag: Tag) => {
      tagData + (tag.getKey -> tag.getValue)
    })
  }

  def load(s3: AmazonS3,bucket: String, folder: String, slug: String):S3StaticAsset={
    val key = s"${S3StaticAsset.staticPageFolder}$slug"
    val obj = s3.getObject(S3StaticAsset.bucket, key)
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    val taggingResult = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, key))
    taggingResult.getTagSet.foldLeft(Map.empty[String, String])((tagData: Map[String, String], tag: Tag) => {
      tagData + (tag.getKey -> tag.getValue)
    })
    S3StaticAsset()
  }

  def save(s3: AmazonS3, bucket: String, folder: String, slug: String, tags: Map[String, String], content: String):String = {
    val key = s"$folder$slug"
    val bytes = content.getBytes
    val inStream = new ByteArrayInputStream(bytes)
    val meta = new ObjectMetadata()
    meta.setContentLength(bytes.length)
    val ts = tags.map { case (k: String, v: String) => new Tag(k, v) }.toList
    val putReq = new PutObjectRequest(bucket, key, inStream, meta).withTagging(new ObjectTagging(ts))
    val result: PutObjectResult = s3.putObject(putReq)

    result.getVersionId
  }
}
