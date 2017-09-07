package prowse.github.cosm1c.healthmesh.usercount

import akka.NotUsed
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}

object CountFlow {

    def create()(implicit materializer: ActorMaterializer): (SourceQueueWithComplete[Int], Source[Int, NotUsed]) =
        Source.queue[Int](0, OverflowStrategy.backpressure)
            .conflate(_ + _)
            .scan(0)(_ + _)
            .toMat(BroadcastHub.sink /* TODO: setting a large buffer mitigates race condition, would prefer: (bufferSize = 1)*/)(Keep.both)
            .run()

}
