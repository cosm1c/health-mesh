package prowse.github.cosm1c.healthmesh.poller

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController._
import prowse.github.cosm1c.healthmesh.poller.ComponentPollerActor._
import prowse.github.cosm1c.healthmesh.util.Status.{Failure, SUCCESS_DONE, Success}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{FiniteDuration, _}

object HealthPollerMediatorActor {

    def props(deltaStream: DeltaStreamController, defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit materializer: Materializer, clock: Clock, executionContext: ExecutionContext): Props =
        Props(new HealthPollerMediatorActor(deltaStream, defaultPollInterim, interimOverideDuration, pollHistorySize))

    object ListNodes

    case class UpdatePollInterval(nodeId: String, maybePollInterimMs: Option[Long], maybePollNow: Option[Boolean])

    case class PutPoller(nodeInfo: NodeInfo /* TODO: pollingInstructions */)

    case class DelPoller(nodeId: String)

    case class FetchPollHistory(nodeId: String)

    val emptyPollHistory = PollHistory(immutable.Seq.empty[PollResult])
}

class HealthPollerMediatorActor(deltaStream: DeltaStreamController, defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit val materializer: Materializer, clock: Clock, executionContext: ExecutionContext) extends Actor with ActorLogging {

    import HealthPollerMediatorActor._

    private implicit val timeout = Timeout(2.seconds)
    private var healthPollers = Map.empty[String, ActorRef]

    deltaStream.deltaSource.runForeach { delta =>
        delta.del.foreach(nodeInfo => self ! DelPoller(nodeInfo._1))
        delta.add.foreach(nodeInfo => self ! PutPoller(nodeInfo._2))
    }

    override def receive: Receive = {

        case PollResult(nodeInfo) =>
            deltaStream.add(nodeInfo)
            ()

        case ListNodes =>
            sender() ! NodeList(healthPollers.keySet)

        case PutPoller(nodeInfo) =>
            if (!healthPollers.contains(nodeInfo.id)) {
                healthPollers += nodeInfo.id -> context.actorOf(ComponentPollerActor.props(nodeInfo, new DummyHealthPoller(nodeInfo), defaultPollInterim, interimOverideDuration, pollHistorySize))
            }

        case DelPoller(id) =>
            healthPollers.get(id).foreach { actorRef =>
                actorRef ! PoisonPill
                healthPollers -= id
            }

        case FetchPollHistory(nodeId) =>
            healthPollers.get(nodeId) match {
                case Some(actor) =>
                    val origSender = sender()
                    (actor ? ListPollHistory).mapTo[PollHistory]
                        .foreach(history => origSender ! Success(history))

                case None =>
                    sender() ! Failure[PollHistory](new RuntimeException(s"Unknown node $nodeId"))
            }

        case UpdatePollInterval(nodeId, maybePollInterimMs, maybePollNow) =>
            healthPollers.get(nodeId) match {
                case Some(poller) =>
                    maybePollInterimMs.foreach(pollInterimMs => poller ! SetInterimOverride(FiniteDuration(pollInterimMs, TimeUnit.MILLISECONDS)))
                    maybePollNow.foreach(pollNow => if (pollNow) poller ! PollNow)
                    sender() ! SUCCESS_DONE

                case None =>
                    sender() ! Failure[Done](new RuntimeException(s"Unknown node $nodeId"))
            }

        case msg => log.warning("Received unexpected message: {}", msg)
    }
}
