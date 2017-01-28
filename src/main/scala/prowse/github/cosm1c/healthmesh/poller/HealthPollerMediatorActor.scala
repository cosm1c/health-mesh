package prowse.github.cosm1c.healthmesh.poller

import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.stream.Materializer
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.NodeInfo

object HealthPollerMediatorActor {

    def props(deltaStream: DeltaStreamController)(implicit materializer: Materializer, clock: Clock): Props =
        Props(new HealthPollerMediatorActor(deltaStream))

    case class AddPoller(nodeInfo: NodeInfo /* TODO: pollingInstructions */)

    case class DelPoller(id: String)

    case class PollResult(nodeInfo: NodeInfo)

}

class HealthPollerMediatorActor(deltaStream: DeltaStreamController)(implicit val materializer: Materializer, clock: Clock) extends Actor with ActorLogging {

    import HealthPollerMediatorActor._

    private var healthPollers = Map.empty[String, ActorRef]

    deltaStream.deltaSource.runForeach { delta =>
        delta.del.foreach(nodeInfo => self ! DelPoller(nodeInfo._1))
        delta.add.foreach(nodeInfo => self ! AddPoller(nodeInfo._2))
    }

    override def receive: Receive = {

        case PollResult(nodeInfo) =>
            deltaStream.add(nodeInfo)
            ()

        case AddPoller(nodeInfo) =>
            if (!healthPollers.contains(nodeInfo.id)) {
                healthPollers += nodeInfo.id -> context.actorOf(ComponentPollerActor.props(nodeInfo))
            }

        case DelPoller(id) =>
            healthPollers.get(id).foreach { actorRef =>
                actorRef ! PoisonPill
                healthPollers -= id
            }

        case msg => log.warning("Received unexpected message: {}", msg)
    }
}
