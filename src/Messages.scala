abstract class Message

case class InitCommit(txId: Int)
case class AgreeReq(txId: Int)
case class Prepare(txId: Int)
case class Rollback(txId: Int)
case class Commit(txId: Int)
case class Agree(txId: Int)
case class Abort(txId: Int)
case class PrepareOk(txId: Int)
case class Ack(txId: Int)