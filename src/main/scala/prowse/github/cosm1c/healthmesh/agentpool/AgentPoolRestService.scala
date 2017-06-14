package prowse.github.cosm1c.healthmesh.agentpool

import javax.ws.rs.Path

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import io.swagger.annotations._
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.{FetchAgentConfig, ListAgents, PostAgentRequest, RemoveAgent, UpdateAgentConfig}
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolRestService.{AgentDeletedResponse, AgentNotFoundResponse}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleConfig, ExampleRequestPayload, ExampleResponsePayload}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object AgentPoolRestService {

    val AgentNotFoundResponse: StandardRoute = Directives.complete(HttpResponse(status = StatusCodes.NotFound))

    val AgentConfigUpdatedResponse: StandardRoute = Directives.complete(HttpResponse(status = StatusCodes.NoContent))

    val AgentDeletedResponse: StandardRoute = Directives.complete(HttpResponse(status = StatusCodes.OK))
}

@Api(value = "/agents", produces = "application/json")
@Path("/agents")
class AgentPoolRestService(agentPoolActor: ActorRef)(implicit val mat: Materializer) extends Directives with ExampleAgent.JsonSupport {

    private implicit val timeout = Timeout(1.second)

    val route: Route =
        pathPrefix("agents") {
            postAgentMessage ~
                putAgentConfig ~
                delAgent ~
                listAgents ~
                getAgentConfig
        }

    @ApiOperation(value = "Fetch set of all agent ids", httpMethod = "GET", responseContainer = "Set", response = classOf[String])
    def listAgents: Route =
        get {
            pathEndOrSingleSlash {
                val eventualAgentsList = (agentPoolActor ? ListAgents).mapTo[Set[String]]
                onSuccess(eventualAgentsList) { agentsList =>
                    complete(agentsList)
                }
            }
        }

    @ApiOperation(value = "Create or Update an Agent with supplied config", httpMethod = "PUT", response = classOf[ExampleConfig])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataType = "string", paramType = "path"),
        new ApiImplicitParam(name = "agentConfig", value = "Agent config", required = true, dataType = "prowse.github.cosm1c.healthmesh.agentpool.AgentActor.AgentConfig", paramType = "body")
    ))
    @Path("/{agentId}")
    def putAgentConfig: Route =
        put {
            path(RemainingPath) { agentPath =>
                entity(as[ExampleConfig]) { agentConfig =>
                    val eventualStatus = (agentPoolActor ? UpdateAgentConfig(agentPath.toString(), agentConfig)).mapTo[Try[ExampleConfig]]
                    onSuccess(eventualStatus) {

                        case Success(updatedAgentConfig) => complete(updatedAgentConfig)

                        case Failure(cause) => failWith(cause)
                    }
                }
            }
        }

    @ApiOperation(value = "Fetch Agent config", httpMethod = "GET", response = classOf[ExampleConfig])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataType = "string", paramType = "path")
    ))
    @Path("/{agentId}")
    def getAgentConfig: Route =
        get {
            path(RemainingPath) { agentPath =>
                val eventualStatus = (agentPoolActor ? FetchAgentConfig(agentPath.toString())).mapTo[Option[Try[ExampleConfig]]]
                onSuccess(eventualStatus) {

                    case Some(Success(agentConfig)) => complete(agentConfig)

                    case None => AgentNotFoundResponse

                    case Some(Failure(cause)) => failWith(cause)
                }
            }
        }

    @ApiOperation(value = "Delete Agent", httpMethod = "DELETE", response = classOf[ExampleConfig])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataType = "string", paramType = "path")
    ))
    @Path("/{agentId}")
    def delAgent: Route =
        delete {
            path(RemainingPath) { agentPath =>
                val eventualStatus = (agentPoolActor ? RemoveAgent(agentPath.toString())).mapTo[Try[Done]]
                onSuccess(eventualStatus) {

                    case Success(_) => AgentDeletedResponse

                    case Failure(cause) => failWith(cause)
                }
            }
        }

    @ApiOperation(value = "Delete Agent", httpMethod = "POST")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataType = "string", paramType = "path"),
        new ApiImplicitParam(name = "message", value = "Message to send to actor", paramType = "body")
    ))
    @Path("/{agentId}")
    def postAgentMessage: Route =
        post {
            path(RemainingPath) { agentPath =>
                entity(as[ExampleRequestPayload]) { exampleRequest =>
                    complete {
                        (agentPoolActor ? PostAgentRequest(agentPath.toString(), exampleRequest)).mapTo[ExampleResponsePayload]
                    }
                }
            }
        }
}
