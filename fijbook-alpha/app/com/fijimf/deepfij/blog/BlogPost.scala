package com.fijimf.deepfij.blog

import java.time.LocalDateTime

import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import play.api.Logger

import scala.util.Try


case class BlogPost
(
  id: String,
  slug: String,
  title: String,
  subtitle: String,
  author: String,
  date: LocalDateTime,
  sections: List[BlogSection],
  tags: List[String],
  visible: Boolean,
  releaseDate: LocalDateTime
) {


}

object BlogPost {
  val log = Logger(this.getClass)

  implicit val localDateDecoder: Decoder[LocalDateTime] = Decoder.decodeString.emapTry(s => Try(LocalDateTime.parse(s)))
  implicit val localDateEncoder: Encoder[LocalDateTime] = Encoder.encodeString.contramap(d => d.toString)
  implicit val blogSectionDecoder: Decoder[BlogSection] = deriveDecoder
  implicit val blogSectionEncoder: Encoder[BlogSection] = deriveEncoder
  implicit val blogPostDecoder: Decoder[BlogPost] = deriveDecoder
  implicit val blogPostEncoder: Encoder[BlogPost] = deriveEncoder

  def fromJson(json: String): Option[BlogPost] = {
    parse(json) match {
      case Left(f) =>
        log.warn("Failed parsing JSON:" + f.message)
        None
      case Right(j) => j.as[BlogPost] match {
        case Left(x) => log.warn("Failed decoding parse JSON:"+x.message)
          None
        case Right(b) => Some(b)
      }
    }
  }


  def toJson(b: BlogPost): String = b.asJson.spaces2
}


