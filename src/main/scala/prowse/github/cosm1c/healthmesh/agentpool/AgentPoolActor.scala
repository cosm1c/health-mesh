package prowse.github.cosm1c.healthmesh.agentpool

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import prowse.github.cosm1c.healthmesh.agentpool.NodeMonitorActor._
import prowse.github.cosm1c.healthmesh.flows.MapDeltaFlow.MapDelta
import prowse.github.cosm1c.healthmesh.util.ReplyStatus

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object AgentPoolActor extends SprayJsonSupport {

    def props(agentCreator: NodeDetails => ActorRef): Props =
        Props(new AgentPoolActor(agentCreator))

    final val SUCCESS_DONE = Success(Done)

    final case class MembershipDelta(members: MapDelta[String, NodeDetails])

    final case object FetchConfig

    final case object ListAgents

    final case class AgentPollNow(id: String)

    final case class FetchAgentConfig(agentId: String)

    private final case class AgentRemoved(agentId: String)


    private def lifecycleActorProps(agentId: String, child: ActorRef): Props = Props(new AgentLifecycleActor(agentId, child))

    private class AgentLifecycleActor(agentId: String, child: ActorRef) extends Actor with ActorLogging {

        context.watch(child)

        override def receive: Receive = {

            case Terminated(`child`) =>
                context.parent ! MapDelta[String, NodeDetails](removed = Set(agentId))
                context.stop(self)
        }
    }

}

class AgentPoolActor(agentCreator: NodeDetails => ActorRef) extends Actor with ActorLogging {

    import prowse.github.cosm1c.healthmesh.agentpool.AgentPoolActor._

    private implicit val timeout: Timeout = Timeout(1.second)
    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    @SuppressWarnings(Array("org.wartremover.warts.Var"))
    private var pool = Map.empty[String, ActorRef]

    override def receive: Receive = {

        case AgentPollNow(id) =>
            pool.get(id) match {
                case Some(actorRef) =>
                    actorRef ! PollNow
                    sender() ! ReplyStatus.Success

                case None =>
                    sender() ! ReplyStatus.Failure
            }

        case MembershipDelta(delta) =>
            delta.removed
                .map(pool.get)
                .foreach(_.foreach(context.stop))
            pool = pool -- delta.removed

            val eventualUpdateResults = pool.keySet.intersect(delta.updated.keySet)
                .map(id => pool(id) ? UpdateNodeDetails(delta.updated(id)))

            pool = pool ++
                delta.updated.keySet.diff(pool.keySet)
                    .map { id =>
                        val childAgent = agentCreator(delta.updated(id))
                        context.actorOf(lifecycleActorProps(id, childAgent), s"AgentLifecycle-$id")
                        id -> childAgent
                    }

            if (eventualUpdateResults.isEmpty) {
                sender() ! ReplyStatus.Success

            } else {
                val origSender = sender()
                Future.sequence(eventualUpdateResults)
                    .onComplete {
                        case Success(_) => origSender ! ReplyStatus.Success

                        case Failure(_) => origSender ! ReplyStatus.Failure
                    }
            }

        case AgentRemoved(agentId) => pool -= agentId

        case FetchAgentConfig(id) =>
            pool.get(id) match {
                case Some(actorRef) =>
                    pipe(
                        (actorRef ? FetchConfig).mapTo[Try[NodeDetails]]
                            .map(Some(_))
                    ).to(sender())
                    ()

                case None => sender() ! Success(None)
            }

        case ListAgents => sender() ! pool.keySet
    }

}
