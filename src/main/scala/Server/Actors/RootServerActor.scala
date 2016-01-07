package Server.Actors

import Server.RootService
import akka.actor.{Actor, ActorLogging}

class RootServerActor extends Actor with RootService with ActorLogging {
  def actorRefFactory = context

  def receive = runRoute(myRoute)
}
