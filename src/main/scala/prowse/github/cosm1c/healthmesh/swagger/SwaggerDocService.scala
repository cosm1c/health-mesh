package prowse.github.cosm1c.healthmesh.swagger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import prowse.github.cosm1c.healthmesh.poller.PollerService

import scala.reflect.runtime.{universe => ru}

class SwaggerDocService(system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
    override implicit val actorSystem: ActorSystem = system
    override implicit val materializer: ActorMaterializer = ActorMaterializer()
    override val apiTypes = Seq(ru.typeOf[PollerService])
    //override val host = "localhost:12345"
    //override val basePath = "/"
    override val info = Info(
        title = "health-mesh",
        //version = "",
        description = "An example of displaying a network of nodes in a digraph with health."
        //contact: Option[Contact] = None,
    )
    //override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
    //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
}
