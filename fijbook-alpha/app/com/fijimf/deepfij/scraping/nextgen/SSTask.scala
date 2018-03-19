package com.fijimf.deepfij.scraping.nextgen

import java.util.UUID

import akka.actor.ActorRef

import scala.concurrent.Future

trait SSTask[T] {
  
  def safeToRun:Future[Boolean]
  
  def id:String = UUID.randomUUID().toString
  
  def name: String

  def run(messageListener:Option[ActorRef]): Future[T]
  
  def cancel= {}
}
