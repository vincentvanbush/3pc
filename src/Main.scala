import akka.actor.{ActorSystem, Props}

/**
  * Created by buszek on 07.01.17.
  */
object Main extends App {
  val system = ActorSystem("3pc")
  val nodes = (0 to 4).map(_ => system.actorOf(Props[Node]))

  (nodes, (0 to 4)).zipped.toList.map {
    case (node, index) => node ! Initialize(index, nodes.toList)
  }
}
