package com.fijimf.deepfij.stats

case class Stat[S](name: String, key: String, defaultValue: Double, higherIsBetter: Boolean, f: S => Double)
