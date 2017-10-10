package prowse.github.cosm1c.healthmesh.faststart

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import rx.RxReactiveStreams
import rx.subjects.{BehaviorSubject, PublishSubject}

class BehaviorBroadcast[A, B](z: B, op: (B, A) => B)(implicit materializer: Materializer) {

    private val in: PublishSubject[A] = PublishSubject.create()
    private val out: BehaviorSubject[B] = BehaviorSubject.create(z)
    in.scan(z, (b: B, a: A) => op(b, a)).subscribe(out)

    val source: Source[B, NotUsed] = Source.fromPublisher(RxReactiveStreams.toPublisher(out.onBackpressureLatest()))

    val queue: SourceQueueWithComplete[A] = Source.queue(1, OverflowStrategy.backpressure).toMat(Sink.foreach(in.onNext))(Keep.left).run()
}
