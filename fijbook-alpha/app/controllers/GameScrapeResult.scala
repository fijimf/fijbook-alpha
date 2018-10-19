package controllers

final case class GameScrapeResult(ids: List[Long] = List.empty[Long], unmappedKeys: List[String] = List.empty[String]) {
}
