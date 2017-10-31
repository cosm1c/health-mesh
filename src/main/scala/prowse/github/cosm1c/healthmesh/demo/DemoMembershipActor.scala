package prowse.github.cosm1c.healthmesh.demo

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.MembershipDelta
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor.NodeDetails
import prowse.github.cosm1c.healthmesh.demo.DemoMembershipActor.{PollNow, numRandomNodes}
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.MapDelta
import prowse.github.cosm1c.healthmesh.util.ReplyStatus

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object DemoMembershipActor {

    def props(poolActor: ActorRef): Props =
        Props(new DemoMembershipActor(poolActor))

    final val numRandomNodes: Int = 20

    final case object PollNow

}

class DemoMembershipActor(poolActor: ActorRef) extends Actor with ActorLogging {

    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher
    private implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

    self ! PollNow

    private val demoInitialDelta = MembershipDelta(MapDelta(
        (0 to numRandomNodes).map { id =>
            val details = new NodeDetails(id.toString, s"${id.toString}-Host")
            details.id -> details
        }.toMap[String, NodeDetails]))

    private val demoRemovableNodeId = (numRandomNodes + 1).toString
    private val demoAddDelta = MembershipDelta(MapDelta(updated = Map(demoRemovableNodeId -> new NodeDetails(demoRemovableNodeId, s"$demoRemovableNodeId-Host"))))
    private val demoRemoveDelta = MembershipDelta(MapDelta[String, NodeDetails](removed = Set(demoRemovableNodeId)))

    override def receive: Receive = {
        case PollNow =>
            processDeltaResponse((poolActor ? demoInitialDelta).mapTo[ReplyStatus.Status])
            context.become(randomAdd)
    }

    private def randomAdd: Receive = {
        case PollNow =>
            processDeltaResponse((poolActor ? demoAddDelta).mapTo[ReplyStatus.Status])
            context.become(randomRemove)
    }

    private def randomRemove: Receive = {
        case PollNow =>
            processDeltaResponse((poolActor ? demoRemoveDelta).mapTo[ReplyStatus.Status])
            context.become(randomAdd)
    }

    private def processDeltaResponse(eventualStatus: Future[ReplyStatus.Status]): Unit = {
        eventualStatus.foreach(_ => context.system.scheduler.scheduleOnce(8.seconds, self, PollNow))
        eventualStatus.onComplete {
            case Success(ReplyStatus.Success) =>
                ()

            case Success(ReplyStatus.Failure) =>
                log.error("Failed to send delta due to failure")
                context.system.scheduler.scheduleOnce(1.minute, self, PollNow)

            case Failure(e) =>
                log.error(e, "Failed to send delta due to exception")
                context.system.scheduler.scheduleOnce(1.minute, self, PollNow)
        }
    }

    /*  For calculating a delta between snapshots
    def calcDelta[K, V](prevState: Map[K, V], currState: Map[K, V]): MapDelta[K, V] = {
        val updated = currState.filterKeys(key => {
            val maybeV = prevState.get(key)
            maybeV.isEmpty || maybeV != currState.get(key)
        })
        val removedKeys = prevState.keySet.intersect(currState.keySet)
        MapDelta(updated, removedKeys)
    }
    */

}
