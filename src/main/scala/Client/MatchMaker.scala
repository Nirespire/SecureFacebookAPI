package Client

import akka.actor.{ActorLogging, ActorRef, Actor}

import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._


class MatchMaker extends Actor with ActorLogging {

  import context.dispatcher

  val profiles = mutable.Set[ActorRef]()

  def receive = {

    case actor: ActorRef =>
      profiles.add(actor)

    case true =>
      val actor1 = profiles.toVector(Random.nextInt(profiles.size))
      var actor2 = profiles.toVector(Random.nextInt(profiles.size))
      do {
        actor2 = profiles.toVector(Random.nextInt(profiles.size))
      } while (actor2 == actor1)

      actor1 ! actor2

      context.system.scheduler.scheduleOnce(Random.nextInt(10) second, self, true)

    case _ =>

  }
}
