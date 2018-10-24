package com.fijimf.deepfij.util

import java.time.{LocalDate, LocalDateTime}

import cats.Eq

trait ModelUtils {

  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals

}
