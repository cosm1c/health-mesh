package prowse.github.cosm1c.healthmesh

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{Healthy, NodeInfo, UnknownHealth}
import prowse.github.cosm1c.healthmesh.deltastream.MeshUpdateJsonProtocol._
import prowse.github.cosm1c.healthmesh.deltastream.{DeltaStreamController, MeshUpdateWebsocketFlow}
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor.{FetchPollHistory, PollHistory}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class AppSupervisorActor extends Actor with ActorLogging with SprayJsonSupport {

    private implicit val actorSystem = context.system
    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val clock = Clock.systemUTC()
    private implicit val _log = log
    private implicit val timeout = Timeout(5 seconds)

    private val deltaStreamController = new DeltaStreamController
    private val healthPollerMediatorActor = context.actorOf(HealthPollerMediatorActor.props(deltaStreamController), "healthPollerMediatorActor")
    // TODO: obtain nodes from somewhere - this is randomly generate
    private val numRandomNodes = 500
    private val numLevelTwoNodes = 40
    deltaStreamController.add(Seq[NodeInfo](
        NodeInfo("A", Healthy, Seq.empty[String], Instant.now(clock)),
        NodeInfo("A", Healthy, Seq.empty[String], Instant.now(clock)),
        NodeInfo("C", Healthy, Seq.empty[String], Instant.now(clock))))
    deltaStreamController.add(0 to numLevelTwoNodes map { id =>
        NodeInfo(
            id.toString,
            UnknownHealth,
            Seq[String](s"${('A' + Random.nextInt(5)).asInstanceOf[Char]}"),
            Instant.now)
    })
    deltaStreamController.add(numLevelTwoNodes to numRandomNodes map { id =>
        NodeInfo(
            id.toString,
            UnknownHealth,
            Seq[String](s"${Random.nextInt(numLevelTwoNodes)}"),
            Instant.now)
    })

    private val route: Flow[HttpRequest, HttpResponse, Any] =
    // WebSocket endpoint first is good for reverse proxy setup
        path("ws") {
            handleWebSocketMessages(MeshUpdateWebsocketFlow.create(deltaStreamController.deltaSource, log))
        } ~
            path("pollHistory" / RemainingPath) { id =>
                complete {
                    (healthPollerMediatorActor ? FetchPollHistory(id.toString)).mapTo[PollHistory]
                }
            } ~
            pathEndOrSingleSlash {
                get {
                    getFromResource("ui/index.html")
                }
            } ~
            /*
            path("buildInfo") {
                get {
                    // Set ContentType as we have pre-calculated JSON response as String
                    complete(HttpEntity(ContentTypes.`application/json`, BuildInfoHelper.buildInfoJson))
                }
            } ~
            */
            path(RemainingPath) { filePath =>
                getFromResource("ui/" + filePath)
            }


    private var bindingFuture: Future[ServerBinding] = _

    override def preStart(): Unit = {
        bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
        bindingFuture.onComplete(serverBinding => log.info("Server online - {}", serverBinding))
    }

    override def postStop(): Unit = {
        bindingFuture
            .flatMap { serverBinding =>
                log.info("Server offline - {}", serverBinding)
                serverBinding.unbind()
            }
        ()
    }

    override def receive: Receive = {
        case msg => log.warning("Received unexpected message: {}", msg)
    }

}
