package com.fijimf.deepfij.stats

/**
  *
  * @param name Human friendly name of the statistic
  * @param key Index friendly name of the statistic
  * @param defaultValue default value
  * @param higherIsBetter necessary for statistic based rankings
  * @param f function to convert an intermediate representation of the statistic to its value
  * @tparam S tyep for the intermediate representation of the statistic
  */
case class Stat[S](name: String, key: String, defaultValue: Double, higherIsBetter: Boolean, f: S => Double)
