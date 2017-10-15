package prowse.github.cosm1c.healthmesh.membership

import akka.stream.Materializer
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentId, ExampleAgentUpdate}
import prowse.github.cosm1c.healthmesh.rx.BehaviorSubjectAdapter
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow.{MembershipCommand, MembershipDelta}

object MembershipFlow {

    type MemberId = ExampleAgentId

    final case class MembershipCommand[T](upsert: Map[MemberId, T] = Map.empty[MemberId, T],
                                          remove: Set[MemberId] = Set.empty[MemberId])

    final case class MembershipDelta[T](members: Map[MemberId, T],
                                        added: Map[MemberId, T],
                                        updated: Map[MemberId, T],
                                        removed: Set[MemberId])

    val emptyMembershipDelta: MembershipDelta[ExampleAgentUpdate] =
        MembershipDelta[ExampleAgentUpdate](Map.empty, Map.empty, Map.empty, Set.empty)
}

class MembershipFlow()(implicit materializer: Materializer) {

    val behaviourBroadcast: BehaviorSubjectAdapter[MembershipCommand[ExampleAgentUpdate], MembershipDelta[ExampleAgentUpdate]] =
        new BehaviorSubjectAdapter[MembershipCommand[ExampleAgent.ExampleAgentUpdate], MembershipDelta[ExampleAgent.ExampleAgentUpdate]](MembershipFlow.emptyMembershipDelta, updateState)

    private def updateState(lastUpdate: MembershipDelta[ExampleAgentUpdate], command: MembershipCommand[ExampleAgentUpdate]): MembershipDelta[ExampleAgentUpdate] = {
        val currRemoved = lastUpdate.members.keySet.intersect(command.remove -- command.upsert.keySet)

        val currMembers = lastUpdate.members -- currRemoved ++ command.upsert

        val (currUpdated, currAdded) = command.upsert.partition {
            case (key, _) => lastUpdate.members.contains(key)
        }

        MembershipDelta(currMembers, currAdded, currUpdated, currRemoved)
    }

    /*
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
    */

}
