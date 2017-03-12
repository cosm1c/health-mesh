package prowse.github.cosm1c.healthmesh.poller

import javax.ws.rs.Path

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import io.swagger.annotations._
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.NodeList
import prowse.github.cosm1c.healthmesh.deltastream.MeshUpdateJsonProtocol._
import prowse.github.cosm1c.healthmesh.poller.ComponentPollerActor.PollHistory
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor.{FetchPollHistory, ListNodes, PollNodeNow}
import prowse.github.cosm1c.healthmesh.util.Status.{Failure, Status, Success}

import scala.concurrent.duration._

@Api(value = "/poller", produces = "application/json")
@Path("/poller")
class PollerService(healthPollerMediatorActor: ActorRef, minAllowedPollInterim: Long) extends Directives with SprayJsonSupport {

    implicit val timeout = Timeout(2.seconds)

    val route: Route =
        pathPrefix("poller") {
            listNodes ~
                fetchPollHistory ~
                pollNodeNow
        }

    @ApiOperation(value = "Fetch set of all nodes being polled", httpMethod = "GET", responseContainer = "Set", response = classOf[String])
    def listNodes: Route =
        pathEndOrSingleSlash {
            get {
                complete {
                    (healthPollerMediatorActor ? ListNodes).mapTo[NodeList]
                }
            }
        }

    @ApiOperation(value = "Fetch polling history of nodeId", httpMethod = "GET")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "nodeId", value = "Id of the node", required = true, dataType = "string", paramType = "path")
    ))
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "successful operation", response = classOf[PollHistory]),
        new ApiResponse(code = 404, message = "Node does not exist")
    ))
    @Path("/{nodeId}")
    def fetchPollHistory: Route = {
        get {
            path(Remaining) { nodeId =>
                val eventualPossibleHistory = (healthPollerMediatorActor ? FetchPollHistory(nodeId)).mapTo[Status[PollHistory]]
                onSuccess(eventualPossibleHistory) {
                    case Success(pollHistory) =>
                        complete(pollHistory)

                    case Failure(throwable) =>
                        complete(HttpResponse(status = NotFound, entity = throwable.getMessage))
                }
            }
        }
    }

    @ApiOperation(value = "Initiate immediate health poll and optionally set a temporary polling interim override", httpMethod = "POST", response = classOf[PollHistory])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "nodeId", value = "Id of the node", required = true, dataType = "string", paramType = "path"),
        new ApiImplicitParam(name = "pollInterimMs", value = "Milliseconds from end of poll to next poll - active for limited time", required = false, dataType = "int", paramType = "query")
    ))
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "successful operation"),
        new ApiResponse(code = 404, message = "Node does not exist")
    ))
    @Path("/{nodeId}")
    def pollNodeNow: Route = {
        post {
            path(Remaining) { nodeId =>
                parameter('pollInterimMs.as[Long].?) { maybePollInterimMs =>
                    validate(intervalValidator(maybePollInterimMs), "pollInterimMs below allowed threshold") {
                        val eventualStatus = (healthPollerMediatorActor ? PollNodeNow(nodeId, maybePollInterimMs)).mapTo[Status[Done]]
                        onSuccess(eventualStatus) {
                            case Success(_) =>
                                complete(OK)

                            case Failure(throwable) =>
                                complete(HttpResponse(status = NotFound, entity = throwable.getMessage))
                        }
                    }
                }
            }
        }
    }

    private def intervalValidator(maybePollInterimMs: Option[Long]): Boolean = maybePollInterimMs match {
        case None =>
            true

        case Some(pollInterimMs) =>
            pollInterimMs >= minAllowedPollInterim
    }
}
