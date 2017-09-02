package prowse.github.cosm1c.healthmesh.agentpool

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentId, ExampleConfig, ExampleRequestPayload, ExampleResponsePayload}
import spray.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Success, Try}

object AgentPoolActor extends SprayJsonSupport {

    def props(agentCreator: ExampleConfig => ActorRef): Props = Props(new AgentPoolActor(agentCreator))

    val SUCCESS_DONE = Success(Done)

    final case object FetchConfig

    final case object ListAgents

    final case class BatchAgentUpdates(updates: Seq[UpdateAgentConfig], removes: Set[ExampleAgentId])

    final case class UpdateAgentConfig(id: ExampleAgentId, config: ExampleConfig)

    final case class FetchAgentConfig(id: ExampleAgentId)

    final case class RemoveAgent(id: ExampleAgentId)

    final case class PostAgentRequest(id: ExampleAgentId, payload: ExampleRequestPayload)

    final case class PostAgentResponse(payload: ExampleResponsePayload)


    private def lifecycleActorProps(agentId: ExampleAgentId, child: ActorRef): Props = Props(new AgentLifecycleActor(agentId, child))

    private class AgentLifecycleActor(agentId: ExampleAgentId, child: ActorRef) extends Actor with ActorLogging {

        context.watch(child)

        override def receive: Receive = {

            case Terminated(`child`) =>
                context.parent ! RemoveAgent(agentId)
                context.stop(self)
        }
    }

}

// TODO: review Supervision strategies
class AgentPoolActor(agentCreator: ExampleConfig => ActorRef) extends Actor with ActorLogging {

    import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor._

    private implicit val timeout: Timeout = Timeout(1.second)
    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    private var pool = Map.empty[ExampleAgentId, ActorRef]

    override def receive: Receive = {

        case PostAgentRequest(id, msg) =>
            pool.get(id) match {
                case Some(actorRef) =>
                    pipe(
                        (actorRef ? msg).mapTo[Try[JsValue]]
                            .map(Some(_))
                    ).to(sender())
                    ()

                case None => sender() ! Success(None)
            }

        case UpdateAgentConfig(id, config) =>
            pool.get(id) match {
                case Some(actorRef) => actorRef forward config

                case None =>
                    val childAgent = agentCreator(config)
                    childAgent forward FetchConfig
                    context.actorOf(lifecycleActorProps(id, childAgent), s"AgentLifecycle-$id")
                    pool += id -> childAgent
            }

        case BatchAgentUpdates(updates, removes) =>
            pool = pool -- removes
            val eventualUpdateResults = updates.map(update =>
                pool.get(update.id) match {
                    case Some(actorRef) => (actorRef ? update.config).mapTo[Success[ExampleConfig]]

                    case None =>
                        val childAgent = agentCreator(update.config)
                        context.actorOf(lifecycleActorProps(update.id, childAgent), s"AgentLifecycle-${update.id}")
                        pool += update.id -> childAgent
                        (childAgent ? FetchConfig).mapTo[Success[ExampleConfig]]
                })
            pipe(Future.sequence(eventualUpdateResults)).to(sender())
            ()

        case FetchAgentConfig(id) =>
            pool.get(id) match {
                case Some(actorRef) =>
                    pipe(
                        (actorRef ? FetchConfig).mapTo[Try[ExampleConfig]]
                            .map(Some(_))
                    ).to(sender())
                    ()

                case None => sender() ! Success(None)
            }

        case RemoveAgent(id) =>
            // TODO: revisit use of PoisonPill and RESPONSE_SUCCESS_DONE
            pool.get(id).foreach { actorRef =>
                actorRef ! PoisonPill
                pool -= id
            }
            sender() ! SUCCESS_DONE

        case ListAgents => sender() ! pool.keySet
    }

}
