package prowse.github.cosm1c.healthmesh.poller

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{HealthStatus, _}
import prowse.github.cosm1c.healthmesh.util.RingBuffer

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object ComponentPollerActor {

    def props(nodeInfo: NodeInfo, defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit clock: Clock): Props =
        Props(new ComponentPollerActor(nodeInfo, defaultPollInterim, interimOverideDuration, pollHistorySize))


    case object PollNow

    case class PollResult(nodeInfo: NodeInfo)


    case object ListPollHistory

    case class PollHistory(history: immutable.Seq[PollResult])


    case object ClearInterimOverride

    case class SetInterimOverride(overidePollInterim: FiniteDuration)

}

class ComponentPollerActor(nodeInfo: NodeInfo, defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit val clock: Clock) extends Actor with ActorLogging {

    import ComponentPollerActor._

    log.debug("Started poller for {}", nodeInfo.id)

    private implicit val executor = context.dispatcher

    private val pollHistory = new RingBuffer[PollResult](pollHistorySize)

    private var maybeOveridePollInterim: Option[FiniteDuration] = None
    private var maybeScheduledPoll: Option[Cancellable] = None
    private var lastPollMillis = clock.millis()

    self ! PollNow

    override def receive: Receive = {

        case PollNow =>
            pollHealth()

        case pollResult: PollResult =>
            lastPollMillis = clock.millis()
            pollHistory.put(pollResult)
            scheduleNextPoll()

        case ClearInterimOverride =>
            maybeOveridePollInterim = None
            rescheduleNextPoll()

        case SetInterimOverride(overidePollInterim) =>
            maybeOveridePollInterim = Some(overidePollInterim)
            context.system.scheduler.scheduleOnce(interimOverideDuration, self, ClearInterimOverride)
            rescheduleNextPoll()

        case ListPollHistory =>
            log.debug("[{}] FetchPollHistory", nodeInfo.id)
            sender() ! PollHistory(pollHistory.get())
    }

    private def scheduleNextPoll() = {
        maybeScheduledPoll.map(_.cancel())
        maybeScheduledPoll = Some(context.system.scheduler.scheduleOnce(maybeOveridePollInterim.getOrElse(defaultPollInterim), self, PollNow))
    }

    private def rescheduleNextPoll() = {
        maybeScheduledPoll.map(_.cancel())
        val millisToNextPoll = lastPollMillis + maybeOveridePollInterim.getOrElse(defaultPollInterim).toMillis - clock.millis()
        if (millisToNextPoll <= 0) {
            pollHealth()
        } else {
            maybeScheduledPoll = Some(context.system.scheduler.scheduleOnce(Duration.create(millisToNextPoll, TimeUnit.MILLISECONDS), self, PollNow))
        }
    }

    private def pollHealth() = {
        debugHealthPoll().foreach(pollResult => {
            self ! pollResult
            context.parent ! pollResult
        })
    }

    // TODO: Replace this with actual polling check
    private var lastHealthStatus: HealthStatus = UnknownHealth

    private def debugHealthPoll(): Future[PollResult] = {
        Future {
            lastHealthStatus = lastHealthStatus match {
                case Healthy => Unhealthy
                case _ => Healthy
            }
            PollResult(NodeInfo(nodeInfo.id, lastHealthStatus, nodeInfo.depends, Instant.now(clock)))
        }
    }
}
