import akka.actor.Actor

/**
  * Created by buszek on 09.01.17.
  */
class Timekeeper extends Actor {
  var forMessage: Message = null

  def receive = {
    case InitTimeout(forMsg) => {
      forMessage = forMsg
      Thread.sleep(5000)
      sender ! Timeout(forMessage)
    }
  }
}
