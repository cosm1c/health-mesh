package prowse.github.cosm1c.healthmesh.websocketflow

import java.time.Instant
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor.{HealthStatus, NodeDetails, NodeState}
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.MapDelta
import prowse.github.cosm1c.healthmesh.websocketflow.ClientWebSocketFlow.{MapDeltaPacket, UserCountPacket}
import spray.json.{DefaultJsonProtocol, JsNumber, JsString, JsValue, RootJsonFormat, deserializationError}

import scala.concurrent.duration.{Duration, FiniteDuration}

trait WebSocketJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
        def write(instant: Instant) = JsNumber(instant.toEpochMilli)

        def read(value: JsValue): Instant = value match {
            case JsNumber(millis) => Instant.ofEpochMilli(millis.longValue())
            case _ => deserializationError("Expected JSNumber for Instant")
        }
    }

    implicit object DurationJsonFormat extends RootJsonFormat[Duration] {
        def write(duration: Duration) = JsNumber(duration.toNanos)

        def read(value: JsValue): FiniteDuration = value match {
            case JsNumber(nanos) => Duration.create(nanos.longValue, NANOSECONDS)
            case _ => deserializationError("Expected JSNumber for Duration")
        }
    }

    implicit object HealthStatusJsonFormat extends RootJsonFormat[HealthStatus.HealthStatusType] {
        def write(obj: HealthStatus.HealthStatusType): JsValue = JsString(obj.toString)

        def read(jsValue: JsValue): HealthStatus.HealthStatusType = jsValue match {
            case JsString(str) => HealthStatus.withName(str)
            case _ => deserializationError("Enum string expected")
        }
    }

    implicit val nodeDetailsFormat: RootJsonFormat[NodeDetails] = jsonFormat3(NodeDetails)
    implicit val nodeStateFormat: RootJsonFormat[NodeState] = jsonFormat6(NodeState)
    implicit val mapDeltaFormat: RootJsonFormat[MapDelta[String, NodeState]] = jsonFormat2(MapDelta[String, NodeState])
    implicit val mapDeltaPacketFormat: RootJsonFormat[MapDeltaPacket] = jsonFormat1(MapDeltaPacket)
    implicit val userCountPacketFormat: RootJsonFormat[UserCountPacket] = jsonFormat1(UserCountPacket)

    /*
        // Map keys must be strings to be valid JSON
        implicit def stringMapFormat[V: JsonFormat]: RootJsonFormat[Map[String, V]] =
            new RootJsonFormat[Map[String, V]] {
                override def write(m: Map[String, V]): JsObject = JsObject {
                    m.map { field =>
                        field._1.toString -> field._2.toJson
                    }
                }

                def read(value: JsValue): Map[String, V] = value match {
                    case x: JsObject => x.fields.map { field =>
                        (JsString(field._1).convertTo[String], field._2.convertTo[V])
                    }(collection.breakOut)
                    case x => deserializationError("Expected Map as JsObject, but got " + x.toString)
                }
            }
    */


}
