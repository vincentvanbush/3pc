import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by buszek on 07.01.17.
  */
object Main extends App {
  val ip = "127.0.0.1"
  val port = Random.nextInt(1000) + 6000
  println(s"Listening on $port")

  val config =
    ConfigFactory.
      parseString(s"akka.remote.netty.tcp.port=${port}\nakka.remote.netty.tcp.hostname=$ip").
      withFallback(ConfigFactory.load())
  val system = ActorSystem("3pc", ConfigFactory.load(config))

  def runLocally(numNodes: Int) = {
    val nodes = (0 until numNodes).map(_ => system.actorOf(Props[Node]))

    (nodes, (0 until numNodes)).zipped.toList.map {
      case (node, index) => node ! Initialize(index, nodes.toList)
    }
  }

  def runRemote = {
    val myNode = system.actorOf(Props[Node], "Node")

    println("\nEnter space-separated port numbers to connect to (or just do nothing)")
    val ports = List(port) ++ scala.io.StdIn.readLine().split(" ").map(p => p.toInt).sortBy(p => p)
    val nodes = ports.map(p => {
      Await.result(system.actorSelection(s"akka.tcp://3pc@$ip:$p/user/Node").resolveOne(10 seconds), 10 seconds)
    })

    while (true) {
      println("Initializing another one...");
      (nodes, (0 until nodes.size)).zipped.toList.map {
        case (node, index) => node ! Initialize(index, nodes)
      }
      scala.io.StdIn.readLine()
    }
  }

  // Run the thing locally for testing purposes
  // runLocally(5)

  // Open a single instance
  runRemote
}
