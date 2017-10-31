package prowse.github.cosm1c.healthmesh.flows

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.kernel.Monoid
import monix.execution.Scheduler
import monix.reactive.subjects.BehaviorSubject
import org.reactivestreams.Processor

trait DeltaOps[STATE, DELTA] {

    def toDelta(state: STATE): DELTA

    def applyDelta(state: STATE, delta: DELTA): STATE
}

// TODO: test Monoid laws against classes used
class DeltaFlow[STATE: Monoid, DELTA: Monoid]()(implicit deltaOps: DeltaOps[STATE, DELTA],
                                                monixScheduler: Scheduler) {

    private val emptyTuple: (STATE, DELTA) = (Monoid[STATE].empty, Monoid[DELTA].empty)

    private def conflateTuple(x: (STATE, DELTA), y: (STATE, DELTA)): (STATE, DELTA) =
        (y._1, Monoid[DELTA].combine(x._2, y._2))

    private val processor: Processor[(STATE, DELTA), (STATE, DELTA)] =
        BehaviorSubject(emptyTuple).toReactive(1)

    val inSink: Sink[DELTA, NotUsed] =
        Flow[DELTA]
            .scan(emptyTuple) {
                case ((prevState, _), delta) => (deltaOps.applyDelta(prevState, delta), delta)
            }
            .conflate(conflateTuple)
            .to(Sink.fromSubscriber(processor))

    val outSource: Source[DELTA, NotUsed] =
        Source.fromPublisher(processor)
            .via(Flow[(STATE, DELTA)]
                .prefixAndTail(1)
                .flatMapConcat { case (head, tail) =>
                    Source.single(
                        head.headOption
                            .map(_._1)
                            .map(deltaOps.toDelta)
                            .getOrElse(Monoid[DELTA].empty)
                    ).concat(tail.map(_._2))
                }
                .conflate(Monoid[DELTA].combine))

}
