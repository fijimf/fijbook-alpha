package com.fijimf.deepfij.models.nstats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result}

case class Scoreboard(date: LocalDate, gs: List[(Game, Result)])
