import akka.actor.ActorRef

abstract class Message

case class Initialize(id: Int, nodes: List[ActorRef]) extends Message
case object CommitReq extends Message
case object AgreeReq extends Message
case object Prepare extends Message
//case object Rollback extends Message
case object Commit extends Message
//case object Agree extends Message
case object Abort extends Message
//case object PrepareOk extends Message
case object Ack extends Message

case class InitTimeout(forMsg: Message) extends Message
case class Timeout(forMsg: Message) extends Message