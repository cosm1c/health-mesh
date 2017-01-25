package prowse.github.cosm1c.healthmesh.deltastream

import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, pimpString}

object MeshUpdateJsonProtocol {

    import DefaultJsonProtocol._
    import JsonFormats._

    private implicit val healthStatusJsonFormat = new JsonFormat[HealthStatus] {
        override def read(json: JsValue): HealthStatus = json match {
            case JsString(Healthy.value) => Healthy
            case JsString(Unhealthy.value) => Unhealthy
            case JsString(UnknownHealth.value) => UnknownHealth
            case _ => UnknownHealth
        }

        override def write(obj: HealthStatus): JsValue = JsString(obj.value)
    }

    private implicit val nodeInfoJsonFormat = DefaultJsonProtocol.jsonFormat4(NodeInfo)
    private implicit val deltaNodeInfoJsonFormat = DefaultJsonProtocol.jsonFormat2(Delta)

    def marshallAsJson(model: Delta): String = deltaNodeInfoJsonFormat.write(model).compactPrint

    def unmarshallJson(text: String): Delta = deltaNodeInfoJsonFormat.read(text.parseJson)
}
