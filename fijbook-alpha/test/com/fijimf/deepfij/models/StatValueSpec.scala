package com.fijimf.deepfij.models

import java.time.LocalDate

import org.scalatest.FlatSpec

class StatValueSpec extends FlatSpec {
    "StatValue should only allow certain keys" should
      "be well defined" in {
      val s = StatValue(0L,"Won-Lost","wins", 1L, LocalDate.now(), 10.0)
      assert(s.modelKey==="Won-Lost")

      try {
        StatValue(0L,"Won Lost","wins", 1L, LocalDate.now(), 10.0)
        fail()
      } catch {
        case x:IllegalArgumentException=> //OK
      }
    }
}
