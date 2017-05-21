package com.fijimf.deepfij.blog
import java.time.LocalDate
import io.circe.{Decoder, Encoder}

import scala.util.Try
import io.circe.generic.auto._

case class BlogSection(title: String, htmlBody: String){
}
