package controllers

import com.fijimf.deepfij.models.{Game, Result}

sealed trait GameMapping {
  def sourceKey:String
}

final case class MappedGame(g: Game) extends GameMapping {
  require(!g.sourceKey.trim.isEmpty)
  override def sourceKey: String = g.sourceKey
}

final case class MappedGameAndResult(g: Game, r: Result) extends GameMapping {
  require(!g.sourceKey.trim.isEmpty)
  override def sourceKey: String = g.sourceKey
}

final case class UnmappedGame(keys: List[String], sourceKey:String) extends GameMapping

