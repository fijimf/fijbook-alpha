package com.fijimf.deepfij.scraping.nextgen.tasks

class TourneyUpdater {
/*
  def updateForTournament(filename: String): Future[List[Tracking]] = {
    updateNcaaTournamentGames(filename).map(_.map(g => Tracking(LocalDateTime.now(), s"$g")))
  }

  def updateNcaaTournamentGames(fileName: String): Future[List[Game]] = {
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream(fileName)).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val tourneyData: Map[Int, (LocalDate, Map[String, (String, Int)])] = lines.foldLeft(Map.empty[Int, (LocalDate, Map[String, (String, Int)])])((data: Map[Int, (LocalDate, Map[String, (String, Int)])], str: String) => {
      str.split(",").toList match {
        case year :: date :: Nil =>
          val y = year.toInt
          val d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          data + (y -> (d, Map.empty[String, (String, Int)]))
        case year :: region :: seed :: key :: Nil =>
          val y = year.toInt
          val s = seed.toInt
          data.get(y) match {
            case Some(mm) =>
              data + (y -> (mm._1, mm._2 + (key -> (region, s))))
            case None => data
          }
        case _ => data
      }
    })

    Future.sequence(tourneyData.flatMap { case (year: Int, tuple: (LocalDate, Map[String, (String, Int)])) => {
      logger.info(s"For year $year tournament started on ${tuple._1} and included ${tuple._2.size} teams.")
      tourneyData.map { case (y, (startDate, seedData)) => {
        dao.loadSchedule(y).flatMap {
          case Some(s) =>
            val gs = s.games.filterNot(g => g.date.isBefore(startDate))
            val updatedGames: List[Game] = gs.flatMap(g => {
              val hk = s.teamsMap(g.homeTeamId).key
              val ak = s.teamsMap(g.awayTeamId).key
              (seedData.get(hk).map(_._2), seedData.get(ak).map(_._2)) match {
                case (Some(hs), Some(as)) => Some(g.copy(homeTeamSeed = Some(hs), awayTeamSeed = Some(as)))
                case _ => None
              }
            })
            dao.updateGames(updatedGames)
          case None =>
            Future.successful(List.empty[Game])
        }
      }
      }
    }
    }.toList).map(_.flatten)
  }

 */
}
