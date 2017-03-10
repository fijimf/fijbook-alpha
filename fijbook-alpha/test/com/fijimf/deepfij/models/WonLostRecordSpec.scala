package com.fijimf.deepfij.models

import org.scalatest.FlatSpec

class WonLostRecordSpec extends FlatSpec {
  "WonLostRecord" should
    "be well defined" in {
    val a = WonLostRecord()
    assert(a.won == 0 && a.lost == 0)
    val b = WonLostRecord(1, 2)
    assert(b.won == 1 && b.lost == 2)
    try {
      val c = WonLostRecord(-1, 0)
      fail
    } catch {
      case x: IllegalArgumentException => //OK
    }
  }
  it should "order won/lost records correctly 1" in {
    val a = WonLostRecord(0, 0)
    val b = WonLostRecord(0, 0)
    assert(a.compare(a, b) == 0)
    assert(b.compare(a, b) == 0)
    assert(a.compare(b, a) == 0)
    assert(b.compare(b, a) == 0)
  }

  it should "order won/lost records correctly 2" in {
    val a = WonLostRecord(1, 1)
    val b = WonLostRecord(0, 0)
    assert(a.compare(a, b) == 1)
    assert(b.compare(a, b) == 1)
    assert(a.compare(b, a) == -1)
    assert(b.compare(b, a) == -1)
  }

  it should "order won/lost records correctly 3" in {
    val a = WonLostRecord(3, 1)
    val b = WonLostRecord(2, 0)
    assert(a.compare(a, b) == 1)
    assert(b.compare(a, b) == 1)
    assert(a.compare(b, a) == -1)
    assert(b.compare(b, a) == -1)
  }

  it should "order won/lost records correctly 4" in {
    val a = WonLostRecord(3, 0)
    val b = WonLostRecord(3, 1)
    assert(a.compare(a, b) == 1)
    assert(b.compare(a, b) == 1)
    assert(a.compare(b, a) == -1)
    assert(b.compare(b, a) == -1)
  }

  it should "order won/lost records correctly 5" in {
    val a = WonLostRecord(0, 0)
    val b = WonLostRecord(0, 1)
    assert(a.compare(a, b) == 1)
    assert(b.compare(a, b) == 1)
    assert(a.compare(b, a) == -1)
    assert(b.compare(b, a) == -1)
  }

  it should "generate winning pct" in {
    assert(WonLostRecord(0, 0).pct.isEmpty)
    assert(WonLostRecord(0, 1).pct.contains(0.0))
    assert(WonLostRecord(1, 0).pct.contains(1.0))
    assert(WonLostRecord(1, 1).pct.contains(0.5))
  }

}
