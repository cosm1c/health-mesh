package prowse.github.cosm1c.healthmesh.deltastream

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.Delta


object MeshUpdateWebsocketFlow {

    def create(deltaSource: Source[Delta, NotUsed], log: LoggingAdapter)(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {

        val inbound: Sink[Message, Any] =
            Sink.foreach {
                case bm: BinaryMessage =>
                    bm.dataStream.runForeach(bin => {
                        log.warning("Ignoring binary WebSocket frame from client: {}", bin)
                    })
                    ()

                case tm: TextMessage =>
                    tm.textStream.runForeach(text => {
                        log.warning("Ignoring text WebSocket frame from client: {}", text)
                    })
                    ()
            }

        val outbound: Source[Message, NotUsed] =
            deltaSource
                .map(MeshUpdateJsonProtocol.marshallAsJson)
                .map(TextMessage.Strict)

        Flow.fromSinkAndSource(inbound, outbound)
    }

}
