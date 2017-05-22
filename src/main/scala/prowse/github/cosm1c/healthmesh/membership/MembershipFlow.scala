package prowse.github.cosm1c.healthmesh.membership

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow._

object MembershipFlow {

    case class MembershipCommand[K, +V](upsert: Map[K, V], remove: Set[K])

    case class MembershipDelta[K, +V](members: Map[K, V],
                                      added: Map[K, V],
                                      updated: Map[K, V],
                                      removed: Set[K])

}

class MembershipFlow[K, V] extends GraphStage[FlowShape[MembershipCommand[K, V], MembershipDelta[K, V]]] {

    private val in: Inlet[MembershipCommand[K, V]] = Inlet("Commands")
    private val out: Outlet[MembershipDelta[K, V]] = Outlet("Deltas")
    private val membershipDeltaZero: MembershipDelta[K, V] = MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty)

    override val shape: FlowShape[MembershipCommand[K, V], MembershipDelta[K, V]] = FlowShape(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

        override def preStart(): Unit = pull(in)

        private var downstreamWaiting = false
        private var lastDeltaSent: MembershipDelta[K, V] = membershipDeltaZero
        private var maybeBufferedDelta: Option[MembershipDelta[K, V]] = Some(membershipDeltaZero)

        def updateState(command: MembershipCommand[K, V]): Unit = {

            val prevMembers = maybeBufferedDelta.getOrElse(lastDeltaSent).members

            val currRemoved = prevMembers.keySet.intersect(command.remove -- command.upsert.keySet)

            val currMembers = prevMembers -- currRemoved ++ command.upsert

            val (currUpdated, currAdded) = command.upsert.partition {
                case (key, _) => prevMembers.contains(key)
            }

            val nextDelta = maybeBufferedDelta
                .map { bufferedDelta =>

                    val (pendingAdds, updates) = currUpdated.partition {
                        case (key, _) => bufferedDelta.added.keySet.contains(key)
                    }

                    val nextAdded = bufferedDelta.added -- currRemoved ++ currAdded ++ pendingAdds

                    val nextUpdated = bufferedDelta.updated -- currRemoved ++ updates

                    val nextRemoved = bufferedDelta.removed -- currAdded.keySet -- currUpdated.keySet

                    MembershipDelta(currMembers, nextAdded, nextUpdated, nextRemoved)
                }
                .getOrElse(MembershipDelta(currMembers, currAdded, currUpdated, currRemoved))

            if ((nextDelta.added.isEmpty && nextDelta.updated.isEmpty && nextDelta.removed.isEmpty) || nextDelta.members == lastDeltaSent.members) {
                maybeBufferedDelta = None
            } else if (downstreamWaiting) {
                pushDelta(nextDelta)
            } else {
                maybeBufferedDelta = Some(nextDelta)
            }
        }

        private def pushDelta(delta: MembershipDelta[K, V]) = {
            downstreamWaiting = false
            lastDeltaSent = delta
            maybeBufferedDelta = None
            push(out, delta)
        }

        setHandler(in, new InHandler {
            override def onPush(): Unit = {
                updateState(grab(in))
                if (downstreamWaiting) {
                    maybeBufferedDelta.foreach(pushDelta)
                }
                pull(in)
            }
        })

        setHandler(out, new OutHandler {
            override def onPull(): Unit =
                maybeBufferedDelta match {
                    case Some(delta) =>
                        pushDelta(delta)

                    case None =>
                        downstreamWaiting = true
                }
        })
    }
}
