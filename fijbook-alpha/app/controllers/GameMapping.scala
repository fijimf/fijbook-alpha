package controllers

import com.fijimf.deepfij.models.{Game, Result}

sealed trait GameMapping

case class MappedGame(g: Game) extends GameMapping

case class MappedGameAndResult(g: Game, r: Result) extends GameMapping

case class UnmappedGame(keys: List[String]) extends GameMapping

