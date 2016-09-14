package com.fijimf.deepfij.models

case class Schedule(season:Season, teams:Map[String, Team], conferences:Map[String, Conference], teamConference:Map[String, String], games:List[Game], results:Map[Long, Result] ) {


}
