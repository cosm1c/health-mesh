package prowse.github.cosm1c.healthmesh.websocketflow

import akka.NotUsed
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Flow, Sink, Source}
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor.NodeState
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.MapDelta

import scala.concurrent.duration._

object ClientWebSocketFlow extends WebSocketJsonSupport {

    private final val keepAlive: String = "{}"

    final case class UserCountPacket(userCount: Int)

    final case class MapDeltaPacket(delta: MapDelta[String, NodeState])

    def clientWebSocketFlow(countSource: Source[Int, NotUsed],
                            deltaSource: Source[MapDelta[String, NodeState], NotUsed]): Flow[Any, TextMessage.Strict, NotUsed] = {

        val deltaFlow =
            deltaSource
                .map(MapDeltaPacket)
                .map(mapDeltaPacketFormat.write(_).compactPrint)

        val userCountFlow =
            countSource
                .map(UserCountPacket)
                .map(userCountPacketFormat.write(_).compactPrint)

        Flow.fromSinkAndSourceCoupled(
            Sink.ignore, // TODO: disconnect on unexpected data from remote websocket?
            userCountFlow.merge(deltaFlow)
                // Throttle to avoid overloading frontend
                .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
                .keepAlive(55.seconds, () => keepAlive)
                .map(TextMessage.Strict)
        )
    }

}
