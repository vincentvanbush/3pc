import akka.actor.Actor

/**
  * Created by buszek on 09.01.17.
  */
class Timekeeper extends Actor {
  var forMessage: Message = null

  def receive = {
    case InitTimeout(forMsg, duration) => {
      forMessage = forMsg
      Thread.sleep(duration)
      sender ! Timeout(forMessage)
    }
  }
}
