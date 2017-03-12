package prowse.github.cosm1c.healthmesh

import java.time.{Clock, Instant, Duration => JDuration}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives.cors
import com.typesafe.config.ConfigFactory
import prowse.github.cosm1c.healthmesh.AppSupervisorActor.javaToScalaDuration
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{Healthy, NodeInfo, UnknownHealth}
import prowse.github.cosm1c.healthmesh.deltastream.{DeltaStreamController, MeshUpdateWebsocketFlow}
import prowse.github.cosm1c.healthmesh.poller.{HealthPollerMediatorActor, PollerService}
import prowse.github.cosm1c.healthmesh.swagger.SwaggerDocService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object AppSupervisorActor {

    // TODO: obtain nodes from somewhere - this is randomly generate
    private val rootName = "ROOT"
    private val numRandomNodes = 20
    private val numLevelTwoNodes = 5

    def addRandomNodes(deltaStreamController: DeltaStreamController)(implicit clock: Clock): Unit = {
        deltaStreamController.add(Seq[NodeInfo](NodeInfo(rootName, Healthy, Seq.empty[String], Instant.now(clock))))
        deltaStreamController.add(Seq[NodeInfo](
            NodeInfo("A", Healthy, Seq(rootName), Instant.now(clock)),
            NodeInfo("B", Healthy, Seq(rootName), Instant.now(clock)),
            NodeInfo("C", Healthy, Seq(rootName), Instant.now(clock)),
            NodeInfo("D", Healthy, Seq(rootName), Instant.now(clock)),
            NodeInfo("E", Healthy, Seq(rootName), Instant.now(clock)),
            NodeInfo("F", Healthy, Seq(rootName), Instant.now(clock))))
        deltaStreamController.add(0 to numLevelTwoNodes map { id =>
            NodeInfo(
                id.toString,
                UnknownHealth,
                Seq[String](s"${('A' + Random.nextInt(4)).asInstanceOf[Char]}"),
                Instant.now)
        })
        deltaStreamController.add(numLevelTwoNodes to numRandomNodes map { id =>
            NodeInfo(
                id.toString,
                UnknownHealth,
                Seq[String](s"${Random.nextInt(numLevelTwoNodes)}"),
                Instant.now)
        })
        ()
    }

    def javaToScalaDuration(jDuration: JDuration): FiniteDuration = FiniteDuration(jDuration.toNanos, TimeUnit.NANOSECONDS)
}

class AppSupervisorActor extends Actor with ActorLogging with SprayJsonSupport {

    private implicit val actorSystem = context.system
    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val clock = Clock.systemUTC()
    private implicit val _log = log
    private implicit val timeout = Timeout(5 seconds)

    private val config = ConfigFactory.load()
    private val deltaStreamController = new DeltaStreamController
    private val healthPollerMediatorActor = context.actorOf(
        HealthPollerMediatorActor.props(deltaStreamController,
            javaToScalaDuration(config.getDuration("poller.defaultPollInterim")),
            javaToScalaDuration(config.getDuration("poller.interimOverideDuration")),
            config.getInt("poller.pollHistorySize")
        ), "healthPollerMediatorActor")

    // TODO: Replace with list of actual nodes
    AppSupervisorActor.addRandomNodes(deltaStreamController)

    private val route: Flow[HttpRequest, HttpResponse, Any] =
    // WebSocket endpoint first is good for reverse proxy setup
        path("ws") {
            handleWebSocketMessages(MeshUpdateWebsocketFlow.create(deltaStreamController.deltaSource, log))
        } ~
            cors()(
                new PollerService(healthPollerMediatorActor, config.getDuration("poller.minAllowedPollInterim").toMillis).route ~
                    new SwaggerDocService(actorSystem).routes
            ) ~
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
