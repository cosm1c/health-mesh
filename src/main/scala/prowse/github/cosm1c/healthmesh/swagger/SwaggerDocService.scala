package prowse.github.cosm1c.healthmesh.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolRestService

class SwaggerDocService(override val basePath: String) extends SwaggerHttpService {
    override val apiClasses = Set(classOf[AgentPoolRestService])
    //override val host = "localhost:12345"
    override val info = Info(
        title = "health-mesh",
        //version = "",
        description = "An example of displaying a network of websocket in a digraph with health."
        //contact: Option[Contact] = None,
    )
    //override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
    //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
}
