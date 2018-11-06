package jobs

import java.time.LocalDateTime

import akka.actor.Actor

class HelloWriter extends Actor {
  override def receive: Receive = {
    case s: String =>
      val str = LocalDateTime.now() + " " + s
      println(str)
      sender ! str
    case f: Any =>
      println(s"Unknown message received ${f.toString}")

  }
}