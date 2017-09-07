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
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, MergeHub, Source}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor.{BatchAgentUpdates, UpdateAgentConfig}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent._
import prowse.github.cosm1c.healthmesh.agentpool.{AgentPoolActor, AgentPoolRestService, ExampleAgent}
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow
import prowse.github.cosm1c.healthmesh.swagger.SwaggerDocService
import prowse.github.cosm1c.healthmesh.usercount.CountFlow
import prowse.github.cosm1c.healthmesh.websocketflow.ClientWebSocketFlow.clientWebSocketFlow

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

object AppSupervisorActor {

    // For demo only
    private def addRandomNodes(agentPoolActor: ActorRef): Unit = {
        implicit val timeout: Timeout = Timeout(1.second)
        val numRandomNodes = 100
        val numLevelTwoNodes = 5

        def randomFlash(): Long = Random.nextInt(800) + 1200L

        agentPoolActor ? BatchAgentUpdates(
            Seq(
                UpdateAgentConfig("A", ExampleConfig("A", "A-Label", Seq(), randomFlash())),
                UpdateAgentConfig("B", ExampleConfig("B", "B-Label", Seq(), randomFlash())),
                UpdateAgentConfig("C", ExampleConfig("C", "C-Label", Seq(), randomFlash())),
                UpdateAgentConfig("D", ExampleConfig("D", "D-Label", Seq(), randomFlash())),
                UpdateAgentConfig("E", ExampleConfig("E", "E-Label", Seq(), randomFlash())),
                UpdateAgentConfig("F", ExampleConfig("F", "F-Label", Seq(), randomFlash()))
            ) ++
                (0 to numLevelTwoNodes).map { id =>
                    UpdateAgentConfig(id.toString,
                        ExampleConfig(
                            id.toString,
                            s"${id.toString}-Label",
                            Seq[String](s"${('A' + Random.nextInt(4)).asInstanceOf[Char]}"),
                            randomFlash()))
                } ++
                (numLevelTwoNodes to numRandomNodes).map { id =>
                    UpdateAgentConfig(id.toString,
                        ExampleConfig(
                            id.toString,
                            s"${id.toString}-Label",
                            Seq[String](s"${Random.nextInt(numLevelTwoNodes)}"),
                            randomFlash()))
                },
            Set[ExampleAgentId]())
        ()
    }
}

class AppSupervisorActor extends Actor with ActorLogging with ExampleAgent.JsonSupport {

    private implicit val actorSystem: ActorSystem = context.system
    private implicit val executionContextExecutor: ExecutionContextExecutor = context.dispatcher
    private implicit val materializer: ActorMaterializer = ActorMaterializer()
    private implicit val clock: Clock = Clock.systemUTC()
    private implicit val httpsContext: HttpsConnectionContext = Http().createClientHttpsContext(AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableSNI(true))))

    private val config = ConfigFactory.load()
    private val httpPort = config.getInt("healthmesh.httpPort")
    private val httpPathPrefix = config.getString("healthmesh.httpPathPrefix")
    private val wsUrl = s"ws://${InetAddress.getLocalHost.getCanonicalHostName}:$httpPort/$httpPathPrefix/ws"
    private val wsUrlResponse = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, s"""{"wsUrl":"$wsUrl"}"""))

    private val (dataSink, createSource) = MembershipFlow.membershipFlow()
    private val mergeHubSink = MergeHub.source.toMat(dataSink)(Keep.left).run()

    private val (counterSourceQueue, counterSource) = CountFlow.create

    private def agentCreator(exampleAgentConfig: ExampleConfig): ActorRef =
        context.actorOf(ExampleAgent.props(exampleAgentConfig, mergeHubSink), s"ExampleAgent-${exampleAgentConfig.id}")

    private val agentPoolActor = context.actorOf(AgentPoolActor.props(agentCreator), "AgentPool")
    private val agentPoolService = new AgentPoolRestService(agentPoolActor)
    AppSupervisorActor.addRandomNodes(agentPoolActor)

    private val route: Route =
        encodeResponse {
            respondWithHeaders(
                `Access-Control-Allow-Origin`.*,
                `Access-Control-Allow-Credentials`(true)
            ) {
                pathPrefix(httpPathPrefix) {
                    path("ws") {
                        onSuccess(createSource())(deltaSource => {
                            val clientFlow = clientWebSocketFlow(counterSource, deltaSource)
                                .prepend(Source.fromIterator(() => {
                                    counterSourceQueue.offer(1)
                                    Iterator.empty
                                }))
                                .watchTermination()((_, done) => done.foreach(_ => counterSourceQueue.offer(-1)))

                            handleWebSocketMessages(clientFlow)
                        })
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
                                    getFromResource("ui/" + filePath)
                                }
                        } ~
                        agentPoolService.route
                } ~
                    new SwaggerDocService(actorSystem).routes
            } ~
                pathEndOrSingleSlash {
                    redirect(Uri('/' + httpPathPrefix + '/'), StatusCodes.PermanentRedirect)
                } ~
                path(httpPathPrefix) {
                    redirect(Uri('/' + httpPathPrefix + '/'), StatusCodes.PermanentRedirect)
                }
        }

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
