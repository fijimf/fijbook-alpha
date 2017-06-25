package controllers

import com.fijimf.deepfij.models.{Game, Result}

sealed trait GameMapping {
  def sourceKey:String
}

case class MappedGame(g: Game) extends GameMapping {
  require(!g.sourceKey.trim.isEmpty)
  override def sourceKey: String = g.sourceKey
}

case class MappedGameAndResult(g: Game, r: Result) extends GameMapping {
  require(!g.sourceKey.trim.isEmpty)
  override def sourceKey: String = g.sourceKey
}

case class UnmappedGame(keys: List[String], sourceKey:String) extends GameMapping

