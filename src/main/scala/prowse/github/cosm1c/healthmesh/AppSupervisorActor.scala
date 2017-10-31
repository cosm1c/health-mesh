package prowse.github.cosm1c.healthmesh

import java.net.InetAddress
import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Credentials`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed, Failure => QueueFailure}
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import monix.execution.Scheduler.Implicits.global
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor._
import prowse.github.cosm1c.healthmesh.agentpool.{AgentPoolActor, AgentPoolRestService, NodeMonitorActor}
import prowse.github.cosm1c.healthmesh.demo.DemoMembershipActor
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.{MapDelta, _}
import prowse.github.cosm1c.healthmesh.flows.{DeltaFlow, MonoidFlow}
import prowse.github.cosm1c.healthmesh.swagger.SwaggerDocService
import prowse.github.cosm1c.healthmesh.websocketflow.ClientWebSocketFlow.clientWebSocketFlow
import prowse.github.cosm1c.healthmesh.websocketflow.WebSocketJsonSupport
import cats.instances.all._
import scala.concurrent.Future

class AppSupervisorActor extends Actor with ActorLogging with WebSocketJsonSupport {

    private implicit val actorSystem: ActorSystem = context.system
    private implicit val materializer: ActorMaterializer = ActorMaterializer()
    private implicit val clock: Clock = Clock.systemUTC()
    private implicit val httpsContext: HttpsConnectionContext = Http().createClientHttpsContext(AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableSNI(true))))

    private val config = ConfigFactory.load()
    private val httpPort = config.getInt("healthmesh.httpPort")
    private val httpPathPrefix = config.getString("healthmesh.httpPathPrefix")
    private val wsUrl = s"ws://${InetAddress.getLocalHost.getCanonicalHostName}:$httpPort/$httpPathPrefix/ws"
    private val wsUrlResponse = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, s"""{"wsUrl":"$wsUrl"}"""))
    private val redirectRoot = redirect(Uri("/" + httpPathPrefix + "/"), StatusCodes.PermanentRedirect)

    private val userCountFlow = new MonoidFlow[Int]
    private val userCountQueue =
        Source.queue(0, OverflowStrategy.backpressure)
            .toMat(userCountFlow.inSink)(Keep.left)
            .run()

    private val membershipDeltaFlow = new DeltaFlow[Map[String, NodeState], MapDelta[String, NodeState]]()
    private val membershipDeltaQueue =
        Source.queue(0, OverflowStrategy.backpressure)
            .toMat(membershipDeltaFlow.inSink)(Keep.left)
            .run()

    private def agentCreator(exampleAgentConfig: NodeDetails): ActorRef =
        context.actorOf(NodeMonitorActor.props(exampleAgentConfig, membershipDeltaQueue), s"ExampleAgent-${exampleAgentConfig.id}")

    private val agentPoolActor = context.actorOf(AgentPoolActor.props(agentCreator), "AgentPool")
    private val agentPoolService = new AgentPoolRestService(agentPoolActor)

    // For demo only
    context.actorOf(DemoMembershipActor.props(agentPoolActor))

    private val route: Route =
        encodeResponse {
            respondWithHeaders(
                `Access-Control-Allow-Origin`.*,
                `Access-Control-Allow-Credentials`(true)
            ) {
                pathPrefix(httpPathPrefix) {
                    path("ws") {
                        onSuccess(userCountQueue.offer(1)) {
                            case Enqueued =>
                                handleWebSocketMessages(
                                    clientWebSocketFlow(userCountFlow.outSource, membershipDeltaFlow.outSource)
                                        .watchTermination()((_, done) => done.foreach(_ => userCountQueue.offer(-1))(context.dispatcher)))

                            case QueueFailure(cause) =>
                                log.error(cause, "Failed to enqueue websocket message - Failure")
                                failWith(cause)

                            case QueueClosed =>
                                log.error("Failed to enqueue websocket message - QueueClosed")
                                failWith(new RuntimeException("Failed to enqueue websocket message - QueueClosed"))

                            case Dropped =>
                                log.error("Packet dropped instead of enqueued - Dropped")
                                failWith(new RuntimeException("Packet dropped instead of enqueued - Dropped"))
                        }
                    } ~
                        get {
                            path("wsUrl") {
                                complete(wsUrlResponse)
                            } ~
                                pathEndOrSingleSlash {
                                    getFromResource("ui/index.html")
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
                                    getFromResource("ui/" + filePath.toString)
                                }
                        } ~
                        agentPoolService.route ~
                        new SwaggerDocService(httpPathPrefix).routes
                }
            } ~
                pathEndOrSingleSlash {
                    redirectRoot
                } ~
                path(httpPathPrefix) {
                    redirectRoot
                }
        }

    @SuppressWarnings(Array("org.wartremover.warts.Var"))
    private var bindingFuture: Future[ServerBinding] = _

    override def preStart(): Unit = {
        bindingFuture = Http().bindAndHandle(route, "0.0.0.0", httpPort)
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
