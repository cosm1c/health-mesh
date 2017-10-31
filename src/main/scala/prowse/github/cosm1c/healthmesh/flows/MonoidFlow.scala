package prowse.github.cosm1c.healthmesh.flows

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.Monoid
import monix.execution.Scheduler
import monix.reactive.subjects.BehaviorSubject
import org.reactivestreams.Processor

class MonoidFlow[A: Monoid]()(implicit monixScheduler: Scheduler) {

    private val processor: Processor[A, A] =
        BehaviorSubject(Monoid[A].empty).toReactive(1)

    val inSink: Sink[A, NotUsed] =
        Flow[A]
            .scan(Monoid[A].empty)(Monoid[A].combine)
            .conflate(Monoid[A].combine).to(Sink.fromSubscriber(processor))

    val outSource: Source[A, NotUsed] =
        Source.fromPublisher(processor)
            .via(Flow[A]
                .conflate(Monoid[A].combine))

}
