package com.fijimf.deepfij.models

import java.io.ByteArrayInputStream
import java.time.{LocalDateTime, ZoneOffset}

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.amazonaws.util.IOUtils
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.joda.time.LocalDate

import scala.collection.JavaConversions._

case class S3BlogMetaData
(
  key: String,
  date: String,
  author: String,
  title: String,
  subTitle: String,
  isPublic: Boolean,
  isDeleted: Boolean,
  keywords: List[String],
  lastModified: LocalDateTime
)

case class S3BlogPost(
                       meta: S3BlogMetaData,
                       content: Array[Byte]
                     )

object S3BlogPost {
  def load(s: AmazonS3, b: S3BlogMetaData): S3BlogPost = {
    val obj = s.getObject(S3BlogPost.bucket, s"${S3BlogPost.blogFolder}${b.key}")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    S3BlogPost(b, content.getBytes())
  }


  private val options = new MutableDataSet()

  // uncomment to set optional extensions
  //o.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

  // uncomment to convert soft-breaks to hard breaks
  //o.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

  private val parser: Parser = Parser.builder(options).build
  private val renderer: HtmlRenderer = HtmlRenderer.builder(options).build

  def renderMarkDown(s: String) = renderer.render(parser.parse(s))


  val DATE = "_publish_date"
  val AUTHOR = "_author"
  val TITLE = "_title"
  val SUBTITLE = "_subtitle"
  val IS_PUBLIC = "_is_public"
  val IS_DELETED = "_is_deleted"
  val TAGS = "_tags"
  val bucket = "fijimf-2017"
  val blogFolder = "blog/"

  def createMetaData(os: S3ObjectSummary, tr: GetObjectTaggingResult): S3BlogMetaData = {
    val key: String = os.getKey
    val lastModified: LocalDateTime = LocalDateTime.ofEpochSecond(os.getLastModified.getTime, 0, ZoneOffset.UTC)
    val tags = tr.getTagSet.foldLeft(Map.empty[String, String])((tagData: Map[String, String], tag: Tag) => {
      tagData + (tag.getKey -> tag.getValue)
    })
    S3BlogMetaData(
      key = key.replace(blogFolder, ""),
      date = tags.getOrElse(DATE, LocalDate.now().toString("yyyy-mm-dd")),
      author = tags.getOrElse(AUTHOR, ""),
      title = tags.getOrElse(TITLE, ""),
      subTitle = tags.getOrElse(SUBTITLE, ""),
      isPublic = tags.get(IS_PUBLIC).exists(_.toLowerCase == "true"),
      isDeleted = tags.get(IS_DELETED).exists(_.toLowerCase == "true"),
      keywords = tags.get(TAGS).map(_.split(",").map(_.trim).toList).getOrElse(List.empty[String]),
      lastModified = lastModified
    )
  }

  def save(s3: AmazonS3, post: S3BlogPost) = {
    val key = s"${S3BlogPost.blogFolder}${post.meta.key}"
    val bytes = post.content
    val inStream = new ByteArrayInputStream(bytes)
    val objMeta = new ObjectMetadata()
    objMeta.setContentLength(bytes.length)
    val s3Tags = List(
      new Tag(DATE, post.meta.date),
      new Tag(AUTHOR, post.meta.author),
      new Tag(TITLE, post.meta.title),
      new Tag(SUBTITLE, post.meta.subTitle),
      new Tag(IS_PUBLIC, post.meta.isPublic.toString),
      new Tag(IS_DELETED, post.meta.isDeleted.toString),
      new Tag(TAGS, post.meta.keywords.mkString(" "))
    )
    val putReq = new PutObjectRequest(bucket, key, inStream, objMeta).withTagging(new ObjectTagging(s3Tags))
    val result: PutObjectResult = s3.putObject(putReq)

    result.getVersionId

  }

  def list(s3: AmazonS3, bucket: String, folder: String): List[S3BlogMetaData] = {
    s3.listObjects(bucket, folder).getObjectSummaries.map(os => {
      val tr = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, os.getKey))
      createMetaData(os, tr)
    }).toList
  }

}
