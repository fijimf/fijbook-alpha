package com.fijimf.deepfij.stats.predictor

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.FlatSpec

class StatValueGameFeatureMapperSpec extends FlatSpec {
  //Can't generally use triple equals cause it means something else in Slick.
  implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.0001)
  "Feature normalizer" should "normalize by z-score for a single element" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.Z_SCORE, List(100.0))
    assert(shift === 100.0)
    assert(scale === 1.0)
  }

  "Feature normalizer" should "normalize by z-score for a simple edge case" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.Z_SCORE, List(0.0, 0.0, 0.0))
    assert(shift === 0.0)
    assert(scale === 1.0)
  }

  "Feature normalizer" should "normalize by z-score for a normal case" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.Z_SCORE, List(100.0, 200.0, 300.0, 400.0, 500.0))
    assert(shift === 300.0)
    assert(scale === 158.1138830084)
  }

  "Feature normalizer" should "normalize by min-max for a single element" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.MIN_MAX, List(100.0))
    assert(shift === 100.0)
    assert(scale === 1.0)
  }

  "Feature normalizer" should "normalize by min-max for a simple edge case" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.MIN_MAX, List(0.0, 0.0, 0.0))
    assert(shift === 0.0)
    assert(scale === 1.0)
  }

  "Feature normalizer" should "normalize by min-max for a normal case" in {
    val (shift, scale) = StatValueGameFeatureMapper.normParms(StatValueGameFeatureMapper.MIN_MAX, List(100.0, 200.0, 300.0, 400.0, 500.0))
    assert(shift === 100.0)
    assert(scale === 400.0)
  }
}
