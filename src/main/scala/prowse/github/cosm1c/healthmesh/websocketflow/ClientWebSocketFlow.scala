package prowse.github.cosm1c.healthmesh.websocketflow

import akka.NotUsed
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Flow, Sink, Source}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentWebsocketPayload, UserCount, conflateExampleAgentWebsocketPayload}
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow

import scala.concurrent.duration._

object ClientWebSocketFlow extends ExampleAgent.JsonSupport {

    private val keepAlive: String = ""

    def clientWebSocketFlow(counterSource: Source[Int, NotUsed],
                            deltaSource: Source[MembershipFlow.MembershipDelta[ExampleAgent.ExampleAgentUpdate], NotUsed]): Flow[Any, TextMessage.Strict, NotUsed] = {

        val deltaFlow = deltaSource
            .prefixAndTail(1)
            .flatMapConcat { case (head, tail) =>
                Source.single(
                    ExampleAgentWebsocketPayload(
                        added = head.headOption.map(_.members).getOrElse(Map.empty),
                        updated = Map.empty,
                        removed = Set.empty)
                ) ++ tail.map(membershipDelta =>
                    ExampleAgentWebsocketPayload(
                        added = membershipDelta.added,
                        updated = membershipDelta.updated,
                        removed = membershipDelta.removed))
            }
            .conflate(conflateExampleAgentWebsocketPayload)
            .map(exampleAgentWebsocketPayloadFormat.write(_).compactPrint)

        val userCountFlow = counterSource
            .conflate(_ + _)
            .map(UserCount)
            .map(userCountFormat.write(_).compactPrint)

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
