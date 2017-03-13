package prowse.github.cosm1c.healthmesh.poller

import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController._

import scala.concurrent.{ExecutionContext, Future}

class DummyHealthPoller(nodeInfo: NodeInfo)(implicit executionContext: ExecutionContext) extends (() => Future[NodeInfo]) {

    // TODO: use last known health to remove duplicates
    private var lastHealthStatus: HealthStatus = UnknownHealth

    override def apply(): Future[NodeInfo] = Future {
        lastHealthStatus = lastHealthStatus match {
            case Healthy => Unhealthy
            case _ => Healthy
        }
        NodeInfo(nodeInfo.id, nodeInfo.label, lastHealthStatus, nodeInfo.depends)
    }
}
