package prowse.github.cosm1c.healthmesh.membership

import akka.NotUsed
import akka.actor.ActorRefFactory
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentId, ExampleAgentUpdate}
import prowse.github.cosm1c.healthmesh.faststart.FastStartBroadcast

import scala.concurrent.Future

object MembershipFlow {

    type MemberId = ExampleAgentId

    case class MembershipCommand[T](upsert: Map[MemberId, T] = Map.empty[MemberId, T],
                                    remove: Set[MemberId] = Set.empty[MemberId])

    case class MembershipDelta[T](members: Map[MemberId, T],
                                  added: Map[MemberId, T],
                                  updated: Map[MemberId, T],
                                  removed: Set[MemberId])

    def membershipFlow()(implicit actorRefFactory: ActorRefFactory, materializer: Materializer): (Sink[MembershipCommand[ExampleAgentUpdate], NotUsed], () => Future[Source[MembershipDelta[ExampleAgentUpdate], NotUsed]]) =
        FastStartBroadcast.dataSinkAndDataSourceGenerator[MembershipCommand[ExampleAgentUpdate], MembershipDelta[ExampleAgentUpdate]](pureExampleAgent, updateState, conflateExampleAgentUpdateDelta)

    private def pureExampleAgent(command: MembershipCommand[ExampleAgentUpdate]): MembershipDelta[ExampleAgentUpdate] =
        MembershipDelta(command.upsert, command.upsert, Map.empty, Set.empty)

    private def updateState(command: MembershipCommand[ExampleAgentUpdate], lastUpdate: MembershipDelta[ExampleAgentUpdate]): MembershipDelta[ExampleAgentUpdate] = {
        val currRemoved = lastUpdate.members.keySet.intersect(command.remove -- command.upsert.keySet)

        val currMembers = lastUpdate.members -- currRemoved ++ command.upsert

        val (currUpdated, currAdded) = command.upsert.partition {
            case (key, _) => lastUpdate.members.contains(key)
        }

        MembershipDelta(currMembers, currAdded, currUpdated, currRemoved)
    }

    private def conflateExampleAgentUpdateDelta(previous: MembershipDelta[ExampleAgentUpdate], current: MembershipDelta[ExampleAgentUpdate]): MembershipDelta[ExampleAgentUpdate] = {
        val (pendingAdds, updates) = current.updated.partition {
            case (key, _) => previous.added.keySet.contains(key)
        }

        val nextAdded = previous.added -- current.removed ++ current.added ++ pendingAdds

        // TODO: conflate instead of overwrite
        val nextUpdated = previous.updated -- current.removed ++ updates

        val nextRemoved = previous.removed -- current.added.keySet -- current.updated.keySet

        MembershipDelta(current.members, nextAdded, nextUpdated, nextRemoved)
    }

}
