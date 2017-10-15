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
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.{AgentPollNow, FetchAgentConfig, ListAgents, PostAgentRequest, RemoveAgent, UpdateAgentConfig}
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolRestService.{AgentDeletedResponse, AgentNotFoundResponse}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleConfig, ExampleRequestPayload, ExampleResponsePayload}
import prowse.github.cosm1c.healthmesh.util.ReplyStatus

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

    private implicit val timeout: Timeout = Timeout(1.second)

    val route: Route =
        pathPrefix("agents") {
            postAgentMessage ~
                putAgentConfig ~
                delAgent ~
                listAgents ~
                getAgentConfig ~
                agentPollNow
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
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataTypeClass = classOf[String], paramType = "path"),
        new ApiImplicitParam(name = "agentConfig", value = "Agent config", required = true, dataTypeClass = classOf[ExampleConfig], paramType = "body")
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
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataTypeClass = classOf[String], paramType = "path")
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
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataTypeClass = classOf[String], paramType = "path")
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

    @ApiOperation(value = "Send message to Agent", httpMethod = "POST")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataTypeClass = classOf[String], paramType = "path"),
        new ApiImplicitParam(name = "message", value = "Message to send to agent actor", paramType = "body", dataTypeClass = classOf[ExampleRequestPayload])
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

    @ApiOperation(value = "Request immediate poll from Agent", httpMethod = "POST")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "agentId", value = "Id of the agent", required = true, dataTypeClass = classOf[String], paramType = "path")
    ))
    @Path("/pollNow/{agentId}")
    def agentPollNow: Route =
        post {
            path("pollNow" / RemainingPath) { agentId =>
                onSuccess((agentPoolActor ? AgentPollNow(agentId.toString)).mapTo[ReplyStatus.Status]) {
                    case ReplyStatus.Success => complete(StatusCodes.OK)
                    case ReplyStatus.Failure => complete(StatusCodes.NotFound)
                }
            }
        }

}
