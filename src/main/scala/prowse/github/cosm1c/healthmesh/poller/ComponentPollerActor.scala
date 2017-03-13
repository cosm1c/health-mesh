package prowse.github.cosm1c.healthmesh.poller

import java.time.Clock
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController._
import prowse.github.cosm1c.healthmesh.util.RingBuffer

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object ComponentPollerActor {

    def props(nodeInfo: NodeInfo, pollHealth: () => Future[NodeInfo], defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit clock: Clock): Props =
        Props(new ComponentPollerActor(nodeInfo, pollHealth, defaultPollInterim, interimOverideDuration, pollHistorySize))


    case object PollNow

    case class PollResult(nodeInfo: NodeInfo)


    case object ListPollHistory

    case class PollHistory(history: immutable.Seq[PollResult])


    case object ClearInterimOverride

    case class SetInterimOverride(overidePollInterim: FiniteDuration)

}

class ComponentPollerActor(nodeInfo: NodeInfo, pollHealth: () => Future[NodeInfo], defaultPollInterim: FiniteDuration, interimOverideDuration: FiniteDuration, pollHistorySize: Int)(implicit val clock: Clock) extends Actor with ActorLogging {

    import ComponentPollerActor._

    log.debug("Started poller for {}", nodeInfo.id)

    private implicit val executor = context.dispatcher

    private val pollHistory = new RingBuffer[PollResult](pollHistorySize)

    private var maybeOverridePollInterim: Option[FiniteDuration] = None
    private var maybeScheduledPoll: Option[Cancellable] = None
    private var lastPollMillis = clock.millis()

    self ! PollNow

    override def receive: Receive = {

        case PollNow =>
            pollNow()

        case pollResult: PollResult =>
            lastPollMillis = clock.millis()
            pollHistory.put(pollResult)
            scheduleNextPoll()

        case ClearInterimOverride =>
            maybeOverridePollInterim = None
            rescheduleNextPoll()

        case SetInterimOverride(overidePollInterim) =>
            maybeOverridePollInterim = Some(overidePollInterim)
            context.system.scheduler.scheduleOnce(interimOverideDuration, self, ClearInterimOverride)
            rescheduleNextPoll()

        case ListPollHistory =>
            log.debug("[{}] FetchPollHistory", nodeInfo.id)
            sender() ! PollHistory(pollHistory.get())
    }

    private def pollNow(): Unit =
        pollHealth().foreach(nodeInfo => {
            val pollResult = PollResult(nodeInfo)
            self ! pollResult
            context.parent ! pollResult
        })

    private def scheduleNextPoll(): Unit = {
        maybeScheduledPoll.map(_.cancel())
        maybeScheduledPoll = Some(context.system.scheduler.scheduleOnce(maybeOverridePollInterim.getOrElse(defaultPollInterim), self, PollNow))
    }

    private def rescheduleNextPoll(): Unit = {
        maybeScheduledPoll.map(_.cancel())
        val millisToNextPoll = lastPollMillis + maybeOverridePollInterim.getOrElse(defaultPollInterim).toMillis - clock.millis()
        if (millisToNextPoll <= 0) {
            pollNow()
        } else {
            maybeScheduledPoll = Some(context.system.scheduler.scheduleOnce(Duration.create(millisToNextPoll, TimeUnit.MILLISECONDS), self, PollNow))
        }
    }
}
