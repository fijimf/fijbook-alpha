package com.fijimf.deepfij.stats.spark

import java.util.Properties
object StatsDbAccess {
  val USER_KEY = "stats.deepfij.user"
  val PASSWORD_KEY = "stats.deepfij.password"

}
trait StatsDbAccess {
  
  val dbProperties: Properties = {
    val props = new java.util.Properties
    props.setProperty("driver", "com.mysql.jdbc.Driver")
    props.setProperty("user", System.getProperty(StatsDbAccess.USER_KEY))
    props.setProperty("password", System.getProperty(StatsDbAccess.PASSWORD_KEY))
    props
  }

}
