package prowse.github.cosm1c.healthmesh.faststart

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRefFactory, Props}
import akka.pattern.ask
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed, Failure => QueueFailure}
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.faststart.FastStartBroadcast._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object FastStartBroadcast {

    private implicit val timeout: Timeout = Timeout(1.second)

    def dataSinkAndDataSourceGenerator[In, Out](pure: In => Out, updateState: (In, Out) => Out, conflate: (Out, Out) => Out)(implicit actorRefFactory: ActorRefFactory, materializer: Materializer): (Sink[In, NotUsed], () => Future[Source[Out, NotUsed]]) = {
        val actor = actorRefFactory.actorOf(FastStartBroadcast.props[In, Out](pure, updateState, conflate))
        val sink: Sink[Data[In], NotUsed] = Sink.actorRefWithAck(actor, OnInitMessage, AckMessage, OnCompleteMessage)
        val input: Flow[In, Data[In], NotUsed] = Flow[In].map(Data(_))
        val sourceGenerator: () => Future[Source[Out, NotUsed]] = () => (actor ? CreateSource).mapTo[Source[Out, NotUsed]]

        (input.to(sink), sourceGenerator)
    }

    final case object CreateSource

    private def props[In, Out](pure: In => Out, updateState: (In, Out) => Out, conflate: (Out, Out) => Out)(implicit materializer: Materializer) =
        Props(new FastStartBroadcast[In, Out](pure, updateState, conflate))

    private case class Data[A](data: A)

    private case object OnInitMessage

    private case object AckMessage

    private case object OnCompleteMessage

}

// TODO: review supervision strategies
class FastStartBroadcast[In, Out](pure: In => Out, updateState: (In, Out) => Out, conflate: (Out, Out) => Out)(implicit materializer: Materializer) extends Actor with ActorLogging {

    private implicit val executionContextExecutor: ExecutionContextExecutor = context.dispatcher

    private var index: Long = 0L
    private var lastElement: (Long, Out) = _

    private val (dataQueue, hubSource) =
        Source.queue[(Long, Out)](0, OverflowStrategy.backpressure)
            .conflate((a, b) => (b._1, conflate(a._2, b._2)))
            .toMat(BroadcastHub.sink(bufferSize = 2))(Keep.both)
            .run()

    hubSource.to(Sink.ignore)

    override def unhandled(message: Any): Unit = {
        log.error("Received unknown message - {}", message)
        context.stop(self)
    }

    override def receive: Receive = {
        case OnInitMessage =>
            context.become(beforeFirstElement)
            sender() ! AckMessage

        case msg =>
            log.error("Expected OnInitMessage but received {}", msg)
            context.stop(self)
    }

    private def beforeFirstElement: Receive = {
        case Data(data) =>
            lastElement = (0, pure(data.asInstanceOf[In]))
            pushData()
            context.become(afterFirstElement)

        case CreateSource => sender() ! hubSource.map(_._2)

        case OnCompleteMessage => context.stop(self)
    }

    private def afterFirstElement: Receive = {
        case Data(data) =>
            index += 1
            lastElement = (index, updateState(data.asInstanceOf[In], lastElement._2))
            pushData()

        case CreateSource =>
            val lastElementIndex = lastElement._1
            val filterStale = hubSource.dropWhile(_._1 <= lastElementIndex)
            sender() ! (Source.single(lastElement) ++ filterStale).map(_._2)
                .conflate(conflate)

        case OnCompleteMessage => context.stop(self)
    }

    private def pushData(): Unit = {
        val origSender = sender()
        dataQueue.offer(lastElement)
            .onComplete({
                case Success(queueOfferResult) => queueOfferResult match {
                    case Enqueued => origSender ! AckMessage

                    case QueueFailure(cause) => failStage(cause)

                    case QueueClosed => completeStage()

                    case Dropped => failStage(new RuntimeException("Packet dropped instead of enqueued"))
                }

                case Failure(cause) => failStage(cause)
            })
    }

    private def completeStage(): Unit = context.stop(self)

    private def failStage(cause: Throwable): Unit = {
        log.error(cause, "FastStartBroadcastActor Failed")
        context.stop(self)
    }
}
