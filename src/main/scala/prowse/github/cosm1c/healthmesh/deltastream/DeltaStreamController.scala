package prowse.github.cosm1c.healthmesh.deltastream

import java.time.Instant

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult, SourceShape}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object DeltaStreamController {

    val maxTimeToFirstMessage: FiniteDuration = 100 milliseconds
    val websocketKeepAliveDuration: FiniteDuration = 50 seconds

    sealed abstract class HealthStatus(val value: String)

    case object UnknownHealth extends HealthStatus("unknown")

    case object Healthy extends HealthStatus("healthy")

    case object Unhealthy extends HealthStatus("unhealthy")


    case class NodeInfo(id: String, healthStatus: HealthStatus, depends: Seq[String], lastUpdate: Instant)

    case class NodeList(nodeIds: Set[String])

    case class Delta(add: immutable.Map[String, NodeInfo] = Map.empty[String, NodeInfo],
                     del: immutable.Map[String, NodeInfo] = Map.empty[String, NodeInfo]) {
        def isEmptyDelta: Boolean = add.isEmpty && del.isEmpty
    }

    val emptyDelta = Delta()

    val zeroDeltaGenerator: () => Delta = () => emptyDelta

    case class SnapshotAndDelta(snapshot: immutable.Map[String, NodeInfo], lastDelta: Delta)

    val zeroSnapshotAndDelta = SnapshotAndDelta(immutable.Map.empty[String, NodeInfo], emptyDelta)
}

class DeltaStreamController()(implicit log: LoggingAdapter, materializer: Materializer) {

    import DeltaStreamController._

    def add(elem: NodeInfo): Future[QueueOfferResult] = add(Seq(elem))

    def add(elems: Seq[NodeInfo]): Future[QueueOfferResult] =
        pushMethod(Delta(add = elems.map(item => item.id -> item).toMap))

    def del(elem: NodeInfo): Future[QueueOfferResult] = del(Seq(elem))

    def del(elems: Seq[NodeInfo]): Future[QueueOfferResult] =
        pushMethod(Delta(del = elems.map(item => item.id -> item).toMap))


    private def conflateDeltas(aggregate: Delta, pending: Delta): Delta =
        Delta(
            add = (aggregate.add -- pending.del.keys) ++ pending.add,
            del = (aggregate.del -- pending.add.keys) ++ pending.del
        )

    private def streamAccum(previousState: SnapshotAndDelta, currentDelta: Delta): SnapshotAndDelta =
        SnapshotAndDelta((previousState.snapshot -- currentDelta.del.keys) ++ currentDelta.add, currentDelta)

    private val (pushMethod, broadcastSource): ((Delta) => Future[QueueOfferResult], Source[SnapshotAndDelta, NotUsed]) =
        Source.queue[Delta](0, OverflowStrategy.fail)
            .keepAlive(maxTimeToFirstMessage, zeroDeltaGenerator)
            .scan(zeroSnapshotAndDelta)(streamAccum)
            .buffer(1, OverflowStrategy.dropHead)
            .mapMaterializedValue(sourceQueue => (delta: Delta) => sourceQueue.offer(delta))
            .toMat(BroadcastHub.sink(bufferSize = 2))(Keep.both)
            .run()

    // drain messages when no one is connected
    broadcastSource.runWith(Sink.ignore)

    val deltaSource: Source[Delta, NotUsed] =
        Source.fromGraph(GraphDSL.create(broadcastSource) { implicit b =>
            statesSource =>
                import GraphDSL.Implicits._

                val snapshot = b.add(
                    Flow[SnapshotAndDelta]
                        .take(1)
                        .map(state => Delta(add = state.snapshot)))

                val deltas = b.add(
                    Flow[SnapshotAndDelta]
                        .drop(1)
                        .map(_.lastDelta))

                val bcast = b.add(Broadcast[SnapshotAndDelta](2))

                val concat = b.add(Concat[Delta]())

                val conflationBuffer = b.add(
                    Flow[Delta]
                        .conflate(conflateDeltas)
                        .filterNot(_.isEmptyDelta)
                        .keepAlive(websocketKeepAliveDuration, zeroDeltaGenerator))

                statesSource ~> bcast ~> snapshot ~> concat ~> conflationBuffer
                /*           */ bcast ~> deltas ~> concat

                SourceShape.of(conflationBuffer.out)
        })

}
