package prowse.github.cosm1c.healthmesh.agentpool

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.HttpsConnectionContext
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, Failure => QueueFailure}
import akka.stream.scaladsl.SourceQueueWithComplete
import cats.Monoid
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.FetchConfig
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor._
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.MapDelta
import spray.json.{JsString, JsValue}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import scala.util.{Random, Success}

object NodeMonitorActor {

    final val pollMillis: Long = 5.seconds.toMillis

    def props(config: NodeDetails, outQueue: SourceQueueWithComplete[MapDelta[String, NodeState]])(implicit clock: Clock, httpsContext: HttpsConnectionContext): Props =
        Props(new NodeMonitorActor(config, outQueue))

    final case class NodeDetails(id: String, serviceName: String, host: String) {
        def this(serviceName: String, host: String) = this(s"$serviceName-$host", serviceName, host)
    }

    object HealthStatus extends Enumeration {
        type HealthStatusType = Value
        val Unknown, Healthy, Unhealthy = Value
    }

    final case class NodeState(details: NodeDetails,
                               healthStatus: HealthStatus.HealthStatusType,
                               lastPollResult: Option[JsValue] = None,
                               depends: Option[Set[String]] = None,
                               lastPollInstant: Option[Instant] = None,
                               lastPollDurationMillis: Option[Long] = None)

    final case object PollNow

    final case class UpdateNodeDetails(nodeDetails: NodeDetails)

    implicit val mapNodeStateMonoid: Monoid[Map[String, NodeState]] = new Monoid[Map[String, NodeState]] {
        override def empty: Map[String, NodeState] = Map.empty[String, NodeState]

        override def combine(x: Map[String, NodeState], y: Map[String, NodeState]): Map[String, NodeState] = x ++ y
    }

}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class NodeMonitorActor(private var details: NodeDetails, outQueue: SourceQueueWithComplete[MapDelta[String, NodeState]])(implicit clock: Clock, httpsContext: HttpsConnectionContext) extends Actor with ActorLogging {

    self ! PollNow

    // So httpContext is used
    httpsContext.toString

    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    private var lastPollMillis = clock.millis()
    private var maybeCancellable: Option[Cancellable] = None

    outQueue.offer(
        MapDelta(updated = Map(
            details.id -> NodeState(details, HealthStatus.Unknown)
        ))
    ).foreach(_ => self ! PollNow)

    override def postStop(): Unit = {
        maybeCancellable.foreach(_.cancel())
        Await.result(
            outQueue.offer(
                MapDelta[String, NodeState](removed = Set(details.id))
            ),
            5.seconds)
        ()
    }

    private val demoDepends: Set[String] =
        (0 to Random.nextInt(5))
            .map(_.toString)
            .toSet

    private var demoHealthIndex = -1

    private def fetchState(): Future[NodeState] = {
        val pollInstant = clock.instant()
        lastPollMillis = pollInstant.toEpochMilli

        // For Demo only
        val demoPollDuration = Random.nextInt(300).toLong
        demoHealthIndex = Random.nextInt(HealthStatus.values.size)
        val promise = Promise[NodeState]()
        context.system.scheduler.scheduleOnce(
            FiniteDuration.apply(demoPollDuration, TimeUnit.MILLISECONDS),
            () => {
                promise.complete(Success(NodeState(
                    details,
                    HealthStatus(demoHealthIndex),
                    Some(JsString(s"Random result at $pollInstant")),
                    Some(demoDepends),
                    Some(pollInstant),
                    Some(demoPollDuration)
                )))
                ()
            })
        promise.future
    }

    override def receive: Receive = {

        case PollNow =>
            fetchState()
                .recover {
                    case t: Throwable => NodeState(details, HealthStatus.Unhealthy, Some(JsString(t.getMessage)))
                }
                .foreach(nodeState =>
                    outQueue.offer(MapDelta(updated = Map(details.id -> nodeState)))
                        .foreach(offerResult => {
                            offerResult match {
                                case QueueFailure(cause) =>
                                    log.error(cause, "Failed to enqueue WebSocket message - Failure")

                                case QueueClosed =>
                                    log.error("Failed to enqueue WebSocket message - QueueClosed")

                                case Dropped =>
                                    log.error("Packet dropped instead of enqueued - Dropped")

                                case _ => ()
                            }
                            scheduleNextPoll()
                        })
                )

        case FetchConfig =>
            sender() ! Success(details)

        case UpdateNodeDetails(updatedDetails) =>
            details = updatedDetails
    }

    private def scheduleNextPoll(): Unit = {
        maybeCancellable.foreach(_.cancel())
        val timeSinceLastFlash = clock.millis() - lastPollMillis
        val delay = if (timeSinceLastFlash > pollMillis) 0 else pollMillis - timeSinceLastFlash
        maybeCancellable = Some(context.system.scheduler.scheduleOnce(delay.millis, self, PollNow))
    }

}
