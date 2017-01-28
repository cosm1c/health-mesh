package prowse.github.cosm1c.healthmesh.deltastream

import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{Delta, NodeInfo}
import spray.json.{DefaultJsonProtocol, pimpString}

object MeshUpdateJsonProtocol {

    import DefaultJsonProtocol._
    import JsonFormats._

    private implicit val nodeInfoJsonFormat = DefaultJsonProtocol.jsonFormat4(NodeInfo)
    private implicit val deltaNodeInfoJsonFormat = DefaultJsonProtocol.jsonFormat2(Delta)

    def marshallAsJson(model: Delta): String = deltaNodeInfoJsonFormat.write(model).compactPrint

    def unmarshallJson(text: String): Delta = deltaNodeInfoJsonFormat.read(text.parseJson)
}
