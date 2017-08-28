package prowse.github.cosm1c.healthmesh.agentpool

import java.time.Clock

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed, Failure => QueueFailure}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.FetchConfig
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent._
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow.{MemberId, MembershipCommand, MembershipDelta}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ExampleAgent {

    def props(config: ExampleConfig, out: Sink[MembershipCommand[ExampleAgentUpdate], NotUsed])(implicit clock: Clock, materializer: Materializer): Props =
        Props(new ExampleAgent(config, out))

    type ExampleAgentId = String

    final case class ExampleConfig(id: ExampleAgentId, label: String, depends: Seq[ExampleAgentId], cssHexColors: Seq[String], flashMillis: Long)

    final case class ExampleRequestPayload(cssHexColors: Seq[String], flashMillis: Long)

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

    final case class ExampleAgentUpdate(label: String,
                                        depends: Seq[MemberId],
                                        cssHexColor: String)

    trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
        implicit val exampleAgentConfigFormat: RootJsonFormat[ExampleConfig] = jsonFormat5(ExampleConfig)
        implicit val exampleRequestFormat: RootJsonFormat[ExampleRequestPayload] = jsonFormat2(ExampleRequestPayload)
        implicit val exampleResponseFormat: RootJsonFormat[ExampleResponsePayload] = jsonFormat1(ExampleResponsePayload)
        implicit val exampleAgentUpdateFormat: RootJsonFormat[ExampleAgentUpdate] = jsonFormat3(ExampleAgentUpdate)
        implicit val membershipDeltaFormat: RootJsonFormat[MembershipDelta[ExampleAgentUpdate]] = jsonFormat4(MembershipDelta[ExampleAgentUpdate])

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
                    case x => deserializationError("Expected Map as JsObject, but got " + x)
                }

            }

        implicit val exampleAgentWebsocketPayloadFormat: RootJsonFormat[ExampleAgentWebsocketPayload] = jsonFormat3(ExampleAgentWebsocketPayload)
    }

    private final case object FlashNow

}

class ExampleAgent(private var config: ExampleConfig, out: Sink[MembershipCommand[ExampleAgentUpdate], NotUsed])(implicit clock: Clock, materializer: Materializer) extends Actor with ActorLogging {

    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher
    private val outQueue = Source.queue(0, OverflowStrategy.backpressure).toMat(out)(Keep.left).run()

    private var cssHexColors = flashableColors(config.cssHexColors)
    private var flashMillis = config.flashMillis
    private var lastFlashMillis = clock.millis()
    private var flashIndex = -1
    private var maybeCancellable: Option[Cancellable] = None

    outQueue.offer(MembershipCommand(upsert = Map(config.id ->
        ExampleAgentUpdate(
            label = config.label,
            depends = config.depends,
            cssHexColor = flashableColors(config.cssHexColors).head
        ))))

    self ! FlashNow

    override def postStop(): Unit = {
        outQueue.offer(MembershipCommand[ExampleAgentUpdate](remove = Set(config.id)))
        ()
    }

    override def receive: Receive = {

        case FlashNow =>
            lastFlashMillis = clock.millis()
            flashIndex = (flashIndex + 1) % cssHexColors.size
            outQueue
                .offer(MembershipCommand(upsert = Map(config.id -> ExampleAgentUpdate(config.label, config.depends, cssHexColors(flashIndex)))))
                .onComplete({
                    case Success(queueOfferResult) => queueOfferResult match {
                        case Enqueued => scheduleNextFlash()

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
            scheduleNextFlash()
            sender() ! Success(config)

        case message@ExampleRequestPayload(updatedCssHexColors, updatedFlashMillis) =>
            cssHexColors = flashableColors(updatedCssHexColors)
            flashMillis = updatedFlashMillis
            flashIndex = -1
            scheduleNextFlash()
            sender() ! ExampleResponsePayload(s"ExampleAgent received $message")

        case FetchConfig =>
            sender() ! Success(config)
    }

    private def scheduleNextFlash(): Unit = {
        maybeCancellable.foreach(_.cancel())
        val timeSinceLastFlash = clock.millis() - lastFlashMillis
        val delay = if (timeSinceLastFlash > flashMillis) 0 else flashMillis - timeSinceLastFlash
        maybeCancellable = Some(context.system.scheduler.scheduleOnce(delay.millis, self, FlashNow))
    }

    private def flashableColors(cssHexColors: Seq[String]): Seq[String] =
        cssHexColors match {
            case Nil =>
                Seq("#008000", "#ff0000")
            case head :: Nil =>
                Seq(head, if (head == "#008000") "#ff0000" else "#008000")
            case _ =>
                cssHexColors
        }
}
