package prowse.github.cosm1c.healthmesh.poller

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging, Props}
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{HealthStatus, Healthy, NodeInfo, Unhealthy}
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor.PollResult

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

    log.info("Started poller for {}", nodeInfo.id)

    private implicit val executor = context.dispatcher

    self ! PollNow

    private var lastHealthStatus: HealthStatus = Healthy

    override def receive: Receive = {

        case PollNow =>
            // TODO: Relace this with actual polling check
            log.info("Random health for {}", nodeInfo.id)
            lastHealthStatus = lastHealthStatus match {
                case Healthy => Unhealthy
                case _ => Healthy
            }
            val delay: FiniteDuration = (5 + Random.nextInt(10)).seconds
            context.system.scheduler.scheduleOnce(delay, self, PollNow)
            context.parent ! PollResult(NodeInfo(nodeInfo.id, lastHealthStatus, nodeInfo.depends, Instant.now(clock)))
    }
}
