package prowse.github.cosm1c.healthmesh

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import prowse.BuildInfoHelper
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.NodeInfo
import prowse.github.cosm1c.healthmesh.deltastream.{DeltaStreamController, MeshUpdateWebsocketFlow}
import prowse.github.cosm1c.healthmesh.poller.HealthPollerMediatorActor

import scala.concurrent.Future
import scala.util.Random

class AppSupervisorActor extends Actor with ActorLogging {

    private implicit val actorSystem = context.system
    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val clocl = Clock.systemUTC()
    private implicit val _log = log

    private val deltaStreamController = new DeltaStreamController
    private val healthPollerMediatorActor = context.actorOf(HealthPollerMediatorActor.props(deltaStreamController), "healthPollerMediatorActor")
    // TODO: obtain list of nodes to poll and replace this debug code
    private val numRandomNodes = 500
    deltaStreamController.add(0 to numRandomNodes map { id =>
        NodeInfo(
            id.toString,
            isHealthy = false,
            Seq.fill(Random.nextInt(5))(Random.nextInt(numRandomNodes).toString),
            Instant.now)
    })

    private val route: Flow[HttpRequest, HttpResponse, Any] =
    // WebSocket endpoint first is good for reverse proxy setup
        path("ws") {
            handleWebSocketMessages(MeshUpdateWebsocketFlow.create(deltaStreamController.deltaSource, log))
        } ~
            pathEndOrSingleSlash {
                get {
                    getFromResource("ui/index.html")
                }
            } ~
            path("buildInfo") {
                get {
                    // Set ContentType as we have pre-calculated JSON response as String
                    complete(HttpEntity(ContentTypes.`application/json`, BuildInfoHelper.buildInfoJson))
                }
            } ~
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
