package prowse.github.cosm1c.healthmesh.deltastream

import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController._
import prowse.github.cosm1c.healthmesh.poller.ComponentPollerActor.{PollHistory, PollResult}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, pimpString}

object MeshUpdateJsonProtocol {

    import DefaultJsonProtocol._

    implicit val healthStatusJsonFormat = new JsonFormat[HealthStatus] {
        override def read(json: JsValue): HealthStatus = json match {
            case JsString(Healthy.value) => Healthy
            case JsString(Unhealthy.value) => Unhealthy
            // case JsString(UnknownHealth.value) => UnknownHealth
            case _ => UnknownHealth
        }

        override def write(obj: HealthStatus): JsValue = JsString(obj.value)
    }

    implicit val nodeInfoJsonFormat: RootJsonFormat[NodeInfo] = DefaultJsonProtocol.jsonFormat4(NodeInfo)
    implicit val nodeListJsonFormat: RootJsonFormat[NodeList] = DefaultJsonProtocol.jsonFormat1(NodeList)
    implicit val deltaNodeInfoJsonFormat: RootJsonFormat[Delta] = DefaultJsonProtocol.jsonFormat2(Delta)

    implicit val pollResultJsonFormat: RootJsonFormat[PollResult] = DefaultJsonProtocol.jsonFormat1(PollResult)
    implicit val pollHistoryJsonFormat: RootJsonFormat[PollHistory] = DefaultJsonProtocol.jsonFormat1(PollHistory)

    def marshallAsJson(model: Delta): String = deltaNodeInfoJsonFormat.write(model).compactPrint

    def unmarshallJson(text: String): Delta = deltaNodeInfoJsonFormat.read(text.parseJson)
}
