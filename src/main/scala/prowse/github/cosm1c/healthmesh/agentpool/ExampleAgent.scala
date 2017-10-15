package prowse.github.cosm1c.healthmesh.agentpool

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed, Failure => QueueFailure}
import akka.stream.scaladsl.SourceQueueWithComplete
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.FetchConfig
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent._
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow.{MemberId, MembershipCommand, MembershipDelta}
import spray.json
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Random, Success}

object ExampleAgent {

    def props(config: ExampleConfig, outQueue: SourceQueueWithComplete[MembershipCommand[ExampleAgentUpdate]])(implicit clock: Clock, httpsContext: HttpsConnectionContext): Props =
        Props(new ExampleAgent(config, outQueue))

    type ExampleAgentId = String

    final case class UserCount(userCount: Int)

    final case class ExampleConfig(id: ExampleAgentId, label: String, depends: Seq[ExampleAgentId], pollMillis: Long)

    final case class ExampleRequestPayload(pollMillis: Long)

    final case class ExampleResponsePayload(message: String)

    final case class ExampleAgentWebsocketPayload(added: Map[MemberId, ExampleAgentUpdate],
                                                  updated: Map[MemberId, ExampleAgentUpdate],
                                                  removed: Set[MemberId])

    def conflateExampleAgentWebsocketPayload(current: ExampleAgentWebsocketPayload, previous: ExampleAgentWebsocketPayload): ExampleAgentWebsocketPayload = {
        val (pendingAdds, updates) = current.updated.partition {
            case (key, _) => previous.added.keySet.contains(key)
        }

        val nextAdded = previous.added -- current.removed ++ current.added ++ pendingAdds

        // TODO: conflate instead of overwrite
        val nextUpdated = previous.updated -- current.removed ++ updates

        val nextRemoved = previous.removed -- current.added.keySet -- current.updated.keySet

        ExampleAgentWebsocketPayload(nextAdded, nextUpdated, nextRemoved)
    }

    object HealthStatus extends Enumeration {
        type HealthStatusType = Value
        val Unknown, Healthy, Unhealthy = Value
    }

    final case class ExampleAgentUpdate(label: String,
                                        depends: Seq[MemberId],
                                        healthStatus: HealthStatus.HealthStatusType,
                                        lastPollInstant: Option[Instant] = None,
                                        lastPollResult: Option[String] = None,
                                        lastPollDurationMillis: Option[Long] = None)

    trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

        implicit val exampleAgentConfigFormat: RootJsonFormat[ExampleConfig] = jsonFormat4(ExampleConfig)
        implicit val exampleRequestFormat: RootJsonFormat[ExampleRequestPayload] = jsonFormat1(ExampleRequestPayload)
        implicit val exampleResponseFormat: RootJsonFormat[ExampleResponsePayload] = jsonFormat1(ExampleResponsePayload)
        implicit val exampleAgentUpdateFormat: RootJsonFormat[ExampleAgentUpdate] = jsonFormat6(ExampleAgentUpdate)
        implicit val membershipDeltaFormat: RootJsonFormat[MembershipDelta[ExampleAgentUpdate]] = jsonFormat4(MembershipDelta[ExampleAgentUpdate])
        implicit val userCountFormat: RootJsonFormat[UserCount] = jsonFormat1(UserCount)

        implicit object HealthStatusJsonFormat extends RootJsonFormat[HealthStatus.HealthStatusType] {
            def write(obj: HealthStatus.HealthStatusType): JsValue = JsString(obj.toString)

            def read(jsValue: JsValue): HealthStatus.HealthStatusType = jsValue match {
                case JsString(str) => HealthStatus.withName(str)
                case _ => json.deserializationError("Enum string expected")
            }
        }

        implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
            def write(instant: Instant) = JsNumber(instant.toEpochMilli)

            def read(value: JsValue): Instant = value match {
                case JsNumber(millis) => Instant.ofEpochMilli(millis.longValue())
                case _ => json.deserializationError("Expected JSNumber for Instant")
            }
        }

        implicit object DurationJsonFormat extends RootJsonFormat[Duration] {
            def write(duration: Duration) = JsNumber(duration.toNanos)

            def read(value: JsValue): FiniteDuration = value match {
                case JsNumber(nanos) => Duration.create(nanos.longValue, NANOSECONDS)
                case _ => json.deserializationError("Expected JSNumber for Duration")
            }
        }

        // Map keys must be strings to be valid JSON
        implicit def intMapFormat[V: JsonFormat]: RootJsonFormat[Map[Int, V]] =
            new RootJsonFormat[Map[Int, V]] {
                override def write(m: Map[Int, V]): JsObject = JsObject {
                    m.map { field =>
                        field._1.toString -> field._2.toJson
                    }
                }

                def read(value: JsValue): Map[Int, V] = value match {
                    case x: JsObject => x.fields.map { field =>
                        (JsString(field._1).convertTo[Int], field._2.convertTo[V])
                    }(collection.breakOut)
                    case x => deserializationError("Expected Map as JsObject, but got " + x.toString)
                }

            }

        implicit val exampleAgentWebsocketPayloadFormat: RootJsonFormat[ExampleAgentWebsocketPayload] = jsonFormat3(ExampleAgentWebsocketPayload)
    }

    final case object PollNow

}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class ExampleAgent(private var config: ExampleConfig, outQueue: SourceQueueWithComplete[MembershipCommand[ExampleAgentUpdate]])(implicit clock: Clock, httpsContext: HttpsConnectionContext) extends Actor with ActorLogging {

    // So httpContext is used
    httpsContext.toString

    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    private var flashMillis = config.pollMillis
    private var lastPollMillis = clock.millis()
    private var healthIndex = -1
    private var maybeCancellable: Option[Cancellable] = None

    outQueue.offer(MembershipCommand(upsert = Map(config.id ->
        ExampleAgentUpdate(
            label = config.label,
            depends = config.depends,
            healthStatus = HealthStatus.Unknown
        )))
    ).foreach(_ => self ! PollNow)

    override def postStop(): Unit = {
        Await.result(
            outQueue.offer(MembershipCommand[ExampleAgentUpdate](remove = Set(config.id))),
            5.seconds)
        ()
    }

    override def receive: Receive = {

        case PollNow =>
            val pollInstant = clock.instant()
            lastPollMillis = pollInstant.toEpochMilli
            healthIndex = Random.nextInt(HealthStatus.values.size)
            outQueue
                .offer(
                    MembershipCommand(
                        upsert = Map(
                            config.id ->
                                ExampleAgentUpdate(
                                    config.label,
                                    config.depends,
                                    HealthStatus(healthIndex),
                                    Some(pollInstant),
                                    Some(s"Random result at $pollInstant"),
                                    Some(Random.nextInt(100).toLong)
                                ))))
                .onComplete({
                    case Success(queueOfferResult) => queueOfferResult match {
                        case Enqueued =>
                            scheduleNextPoll()

                        case QueueFailure(cause) =>
                            log.error(cause, "Failed to enqueue websocket message - Failure")
                            context.stop(self)

                        case QueueClosed =>
                            log.error("Failed to enqueue websocket message - QueueClosed")
                            context.stop(self)

                        case Dropped =>
                            log.error("Packet dropped instead of enqueued - Dropped")
                            context.stop(self)
                    }

                    case Failure(cause) =>
                        log.error(cause, "Packet failed to enqueue")
                        context.stop(self)
                })

        case updatedConfig: ExampleConfig =>
            config = updatedConfig
            scheduleNextPoll()
            sender() ! Success(config)

        case message@ExampleRequestPayload(updatedFlashMillis) =>
            flashMillis = updatedFlashMillis
            healthIndex = -1
            scheduleNextPoll()
            sender() ! ExampleResponsePayload(s"ExampleAgent received $message")

        case FetchConfig =>
            sender() ! Success(config)
    }

    private def scheduleNextPoll(): Unit = {
        maybeCancellable.foreach(_.cancel())
        val timeSinceLastFlash = clock.millis() - lastPollMillis
        val delay = if (timeSinceLastFlash > flashMillis) 0 else flashMillis - timeSinceLastFlash
        maybeCancellable = Some(context.system.scheduler.scheduleOnce(delay.millis, self, PollNow))
    }

}
