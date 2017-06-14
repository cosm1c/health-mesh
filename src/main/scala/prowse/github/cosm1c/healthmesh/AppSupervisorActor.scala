package prowse.github.cosm1c.healthmesh

import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives.cors
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.{BatchAgentUpdates, UpdateAgentConfig}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentWebsocketPayload, ExampleConfig, conflateExampleAgentWebsocketPayload}
import prowse.github.cosm1c.healthmesh.agentpool.{AgentPoolActor, AgentPoolRestService, ExampleAgent}
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow
import prowse.github.cosm1c.healthmesh.swagger.SwaggerDocService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

object AppSupervisorActor {

    // For demo only
    private def addRandomNodes(agentPoolActor: ActorRef): Unit = {
        implicit val timeout = Timeout(1.second)
        val numRandomNodes = 100
        val numLevelTwoNodes = 8

        def randomFlash(): Long = Random.nextInt(500) + 500L

        agentPoolActor ? BatchAgentUpdates(Seq(
            UpdateAgentConfig("A", ExampleConfig("A", "A", Seq(), Seq("#008000"), randomFlash())),
            UpdateAgentConfig("B", ExampleConfig("B", "B", Seq(), Seq("#008000"), randomFlash())),
            UpdateAgentConfig("C", ExampleConfig("C", "C", Seq(), Seq("#008000"), randomFlash())),
            UpdateAgentConfig("D", ExampleConfig("D", "D", Seq(), Seq("#008000"), randomFlash())),
            UpdateAgentConfig("E", ExampleConfig("E", "E", Seq(), Seq("#008000"), randomFlash())),
            UpdateAgentConfig("F", ExampleConfig("F", "F", Seq(), Seq("#008000"), randomFlash()))
        ) ++
            (0 to numLevelTwoNodes).map { id =>
                UpdateAgentConfig(id.toString,
                    ExampleConfig(
                        id.toString,
                        id.toString,
                        Seq[String](s"${('A' + Random.nextInt(4)).asInstanceOf[Char]}"),
                        Seq(),
                        randomFlash()))
            } ++
            (numLevelTwoNodes to numRandomNodes).map { id =>
                UpdateAgentConfig(id.toString,
                    ExampleConfig(
                        id.toString,
                        id.toString,
                        Seq[String](s"${Random.nextInt(numLevelTwoNodes)}"),
                        Seq(),
                        randomFlash()))
            }
        )
        ()
    }
}

class AppSupervisorActor extends Actor with ActorLogging with ExampleAgent.JsonSupport {

    private implicit val actorSystem = context.system
    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val clock = Clock.systemUTC()

    private val (dataSink, createSource) = MembershipFlow.membershipFlow()
    private val mergeHubSink = MergeHub.source.toMat(dataSink)(Keep.left).run()

    private def agentCreator(exampleAgentConfig: ExampleConfig): ActorRef =
        context.actorOf(ExampleAgent.props(exampleAgentConfig, mergeHubSink), s"ExampleAgent-${exampleAgentConfig.id}")

    private val agentPoolActor = context.actorOf(AgentPoolActor.props(agentCreator), "AgentPool")
    private val agentPoolService = new AgentPoolRestService(agentPoolActor)
    AppSupervisorActor.addRandomNodes(agentPoolActor)

    private val route: Flow[HttpRequest, HttpResponse, Any] =
        path("ws") {
            onSuccess(createSource()) { exampleWebsocketMessageSource =>
                handleWebSocketMessages {
                    Flow.fromSinkAndSource(Sink.ignore, exampleWebsocketMessageSource
                        .prefixAndTail(1)
                        .flatMapConcat { case (head, tail) =>
                            Source.single(
                                ExampleAgentWebsocketPayload(
                                    added = head.head.members,
                                    updated = Map.empty,
                                    removed = Set.empty)) ++
                                tail.map(membershipDelta =>
                                    ExampleAgentWebsocketPayload(
                                        added = membershipDelta.added,
                                        updated = membershipDelta.updated,
                                        removed = membershipDelta.removed))
                        }
                        .conflate(conflateExampleAgentWebsocketPayload)
                        .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
                        .map(exampleAgentWebsocketPayloadFormat.write(_).compactPrint)
                        .map(TextMessage.Strict))
                }
            }
        } ~
            cors()(
                agentPoolService.route ~
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
        bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 18080)
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
