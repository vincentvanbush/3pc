import akka.actor.{Actor, ActorRef, Props}

import scala.util.Random

/**
  * Created by buszek on 07.01.17.
  */
class Node extends Actor {
  var nodes: List[ActorRef] = null
  var id: Int = -1

  var sentCommitRequests = null

  var cohortCounter = 0

  def coordinator = nodes.head
  def cohorts = nodes.tail

  def receive = {
    case Initialize(index, nodeList) => {
      id = index
      nodes = nodeList

      if (self == coordinator) {
        context.become(coordinatorIdle)
      } else {
        context.become(cohortIdle)

        // One cohort starts the procedure
        if (id == 1) {
          coordinator ! CommitReq
        }
      }
    }
  }

  // Coordinator states

  def coordinatorIdle: Receive = {
    case CommitReq => {
      println(s"${Console.BLUE}Transaction starts${Console.BLUE}")
      cohorts.foreach(cohort => cohort ! CommitReq)
      context.actorOf(Props[Timekeeper]) ! InitTimeout(AgreeReq)
      context.become(coordinatorWaitForCohortAgree)
    }
  }

  def coordinatorWaitForCohortAgree: Receive = {
    // cohorts agree or abort

    case AgreeReq => {
      cohortCounter += 1
      if (cohortCounter == cohorts.size) {
        cohortCounter = 0
        println(s"${Console.YELLOW}Transaction prepared - all cohorts sent AGREE_REQ${Console.RESET}")
        cohorts.foreach(cohort => cohort ! Prepare)
        context.actorOf(Props[Timekeeper]) ! InitTimeout(Ack)
        context.become(coordinatorWaitForCohortAck)
      }
    }

    case Timeout(AgreeReq) => {
      cohortCounter = 0
      println(s"${Console.RED}Transaction aborted - some cohorts did not send AGREE_REQ in time${Console.RESET}")
      cohorts.foreach(cohort => cohort ! Abort)
      context.become(coordinatorIdle)
    }

    case Abort => {
      cohortCounter = 0
      println(s"${Console.RED}Transaction aborted - ${sender} sent ABORT, expected AGREE_REQ${Console.RESET}")
      cohorts.foreach(cohort => cohort ! Abort)
      context.become(coordinatorIdle)
    }
  }

  def coordinatorWaitForCohortAck: Receive = {
    // cohorts ack or abort

    case Ack => {
      cohortCounter += 1
      if (cohortCounter == cohorts.size) {
        cohortCounter = 0
        println(s"${Console.GREEN}Transaction committed - all cohorts sent ACK, coordinator accepts")
        cohorts.foreach(cohort => cohort ! Commit)
        context.become(cohortIdle)
      }
    }

    case Timeout(Ack) => {
      cohortCounter = 0
      cohorts.foreach(cohort => cohort ! Abort)
      println(s"${Console.RED}Transaction aborted - some cohorts did not send ACK in time${Console.RESET}")
      context.become(coordinatorIdle)
    }
  }

  // Cohort states

  def cohortIdle: Receive = {
    case CommitReq => {
      if (Random.nextInt(30) < 4) { // abort
        println(s"Cohort $id: transaction aborted (answering CommitReq with Abort)")
        sender ! Abort
        context.become(cohortIdle)
      } else { // agree
        println(s"Cohort $id: transaction agreed (answering CommitReq with AgreeReq)")
        sender ! AgreeReq
        context.become(cohortWaitForCoordinatorPrepare)
        context.actorOf(Props[Timekeeper]) ! InitTimeout(Prepare)
      }
    }
  }

  def cohortWaitForCoordinatorPrepare: Receive = {
    case Prepare => {
      println(s"Cohort $id: transaction prepared")
      sender ! Ack
      context.become(cohortWaitForCoordinatorCommit)
      context.actorOf(Props[Timekeeper]) ! InitTimeout(Commit)
    }

    case Timeout(Prepare) => {
      println(s"Cohort $id: transaction aborted (expected Prepare, got Timeout)")
      context.become(cohortIdle)
    }

    case Abort => {
      println(s"Cohort $id: transaction aborted (expected Prepare, got Abort)")
      context.become(cohortIdle)
    }
  }

  def cohortWaitForCoordinatorCommit: Receive = {
    case Commit => {
      println(s"Cohort $id: transaction committed")
      context.become(cohortIdle)
    }

    case Timeout(Commit) => {
      println(s"Cohort $id: transaction committed (expected Commit, got Timeout)")
      context.become(cohortIdle)
    }

    case Abort => {
      println(s"Cohort $id: transaction aborted (expected Commit, got Abort)")
      context.become(cohortIdle)
    }
  }
}
