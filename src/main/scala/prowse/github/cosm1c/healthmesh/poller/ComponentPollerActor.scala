package prowse.github.cosm1c.healthmesh.poller

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging, Props}
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{HealthStatus, Healthy, NodeInfo, Unhealthy}
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor.{FetchPollHistory, PollHistory, PollResult}
import prowse.github.cosm1c.healthmesh.util.RingBuffer

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object ComponentPollerActor {

    def props(nodeInfo: NodeInfo)(implicit clock: Clock): Props =
        Props(new ComponentPollerActor(nodeInfo))

    case object PollNow

    val pollInterim: FiniteDuration = 1 second
}

class ComponentPollerActor(nodeInfo: NodeInfo)(implicit val clock: Clock) extends Actor with ActorLogging {

    import ComponentPollerActor._

    log.debug("Started poller for {}", nodeInfo.id)

    private implicit val executor = context.dispatcher

    self ! PollNow

    private var pollHistory = new RingBuffer[PollResult](5)

    private var lastHealthStatus: HealthStatus = Healthy

    override def receive: Receive = {

        case PollNow =>
            // TODO: Relace this with actual polling check
            log.debug("Random health for {}", nodeInfo.id)
            lastHealthStatus = lastHealthStatus match {
                case Healthy => Unhealthy
                case _ => Healthy
            }

            val delay: FiniteDuration = (5 + Random.nextInt(10)).seconds
            context.system.scheduler.scheduleOnce(delay, self, PollNow)

            recordHealthStatus(lastHealthStatus)

        case FetchPollHistory(_) =>
            log.debug("[{}] FetchPollHistory", nodeInfo.id)
            sender() ! PollHistory(pollHistory.get())
    }

    private def recordHealthStatus(healthStatus: HealthStatus) = {
        val pollResult = PollResult(NodeInfo(nodeInfo.id, healthStatus, nodeInfo.depends, Instant.now(clock)))
        context.parent ! pollResult
        pollHistory.put(pollResult)
    }
}
