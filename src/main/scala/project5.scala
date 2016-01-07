import Client.{BadClientActor, ClientType, MatchMaker}
import Server.Actors.RootServerActor
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.concurrent.duration._
import scala.util.Try

object project5 extends App {
  val config = ConfigFactory.load()
  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)
  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("localhost")
  lazy val numClients = Try(config.getInt("client.numClients")).getOrElse(0)

  // Start up actor system for server
  implicit val serverSystem = ActorSystem("fb-spray-system")
  val service = serverSystem.actorOf(Props[RootServerActor], "fb-REST-service")
  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = serviceHost, port = servicePort)

  Thread.sleep(10000)

  if (numClients > 0) {
    println("Running with " + numClients + " clients")
    println("Start clients!")

    // Start up actor system of clients
    val clientSystem = ActorSystem("client-spray-system")

    val matchmaker = clientSystem.actorOf(Props(new MatchMaker), "MatchMaker")

    val numActive = (0.15 * numClients).toInt
    val numPassive = (0.80 * numClients).toInt
    val numCelebrity = numClients - numActive + numPassive

    (1 to numActive).foreach { idx =>
      val actor = clientSystem.actorOf(Props(new Client.ClientActor(false, ClientType.Active)), "client" + idx)
      matchmaker ! actor
      actor ! true
    }

    (numActive + 1 to numActive + numPassive).foreach { idx =>
      val actor = clientSystem.actorOf(Props(new Client.ClientActor(false, ClientType.Passive)), "client" + idx)
      matchmaker ! actor
      actor ! true
    }

    (numActive + numPassive + 1 to numClients).foreach { idx =>
      val actor = clientSystem.actorOf(Props(new Client.ClientActor(true, ClientType.ContentCreator)), "client" + idx)
//      matchmaker ! actor
      actor ! true
    }

//    (1 to (numClients * 0.05).ceil.toInt).foreach{idx =>
//      val badClient = clientSystem.actorOf(Props(new BadClientActor()), "badclient" + idx)
//      badClient ! true
//    }

    matchmaker ! true
    println("End Loop")
  }
}