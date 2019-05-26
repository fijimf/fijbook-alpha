package com.fijimf.deepfij.models

import java.time.LocalDate

import org.scalatest.FlatSpec

class SeasonSpec extends FlatSpec {
  "Season" should "start on Nov 1" in {
    val start = Season(0L, 2018).startDate
    assert(start.getDayOfMonth===1)
    assert(start.getMonthValue===11)
    assert(start.getYear===2017)
  }

  it should "end on Apr 30" in {
    val start = Season(0L, 2018).endDate
    assert(start.getDayOfMonth===30)
    assert(start.getMonthValue===4)
    assert(start.getYear===2018)
  }

  it should "should have 182 days" in {
    val season = Season(0L, 2018)
    assert(season.dates.size===180)
  }

  it should "should have 183 days in a leap year" in {
    val season = Season(0L, 2020)
    assert(season.dates.size===181)
  }

  it should "identify valid dates" in {
    val season = Season(0L, 2018)
    assert(season.canUpdate(LocalDate.parse("2018-03-31")))
    assert(season.canUpdate(LocalDate.parse("2017-11-30")))
    assert(!season.canUpdate(LocalDate.parse("2017-10-31")))
    assert(!season.canUpdate(LocalDate.parse("2018-05-01")))
    assert(!season.canUpdate(LocalDate.parse("2020-03-31")))

  }


}
