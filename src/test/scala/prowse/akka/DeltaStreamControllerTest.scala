package prowse.akka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController
import prowse.github.cosm1c.healthmesh.deltastream.DeltaStreamController.{Delta, Healthy, NodeInfo, Unhealthy}

class DeltaStreamControllerTest extends FlatSpec with MockitoSugar {

    private implicit val actorSystem = ActorSystem()
    private implicit val materializer = ActorMaterializer()

    private val nodeInfoA1 = NodeInfo("idA", "A", healthStatus = Healthy, Seq())
    private val nodeInfoA2 = NodeInfo("idA", "A", healthStatus = Unhealthy, Seq())
    private val nodeInfoB1 = NodeInfo("idB", "B", healthStatus = Healthy, Seq())
    private val nodeInfoB2 = NodeInfo("idB", "B", healthStatus = Unhealthy, Seq())
    private val nodeInfoC1 = NodeInfo("idC", "C", healthStatus = Healthy, Seq())

    private def withDeltaStream(testCode: (DeltaStreamController) => Any) =
        testCode(new DeltaStreamController)

    private def subscribeWithProbe(source: DeltaStreamController): Probe[Delta] = {
        source.deltaSource.runWith(TestSink.probe[Delta])
    }

    "DeltaSetStream" should "provide initial state" in withDeltaStream { deltaStream =>
        deltaStream.add(nodeInfoA1)

        subscribeWithProbe(deltaStream)
            .ensureSubscription()
            .requestNext(Delta(add = Map(nodeInfoA1.id -> nodeInfoA1)))
    }

    it should "provide initial state for all subscribers" in withDeltaStream { deltaStream =>
        deltaStream.add(nodeInfoA1)
        deltaStream.add(nodeInfoB1)
        deltaStream.add(nodeInfoC1)
        deltaStream.del(nodeInfoA1)
        deltaStream.add(nodeInfoB2)

        subscribeWithProbe(deltaStream)
            .ensureSubscription()
            .requestNext(Delta(add = Map(nodeInfoB1.id -> nodeInfoB2, nodeInfoC1.id -> nodeInfoC1)))

        subscribeWithProbe(deltaStream)
            .ensureSubscription()
            .requestNext(Delta(add = Map(nodeInfoB1.id -> nodeInfoB2, nodeInfoC1.id -> nodeInfoC1)))
    }

    it should "provide ongoing deltas" in withDeltaStream { deltaStream =>
        deltaStream.add(nodeInfoA1)
        deltaStream.add(nodeInfoB1)

        val probe = subscribeWithProbe(deltaStream)
            .ensureSubscription()

        probe.requestNext(Delta(add = Map(nodeInfoA1.id -> nodeInfoA1, nodeInfoB1.id -> nodeInfoB1)))

        deltaStream.del(nodeInfoA1)
        probe.requestNext(Delta(del = Map(nodeInfoA1.id -> nodeInfoA1)))

        deltaStream.add(nodeInfoC1)
        probe.requestNext(Delta(add = Map(nodeInfoC1.id -> nodeInfoC1)))
    }


    it should "provide up to date deltas" in withDeltaStream { deltaStream =>
        deltaStream.add(nodeInfoA1)
        deltaStream.add(nodeInfoB1)

        val probe = subscribeWithProbe(deltaStream)
            .ensureSubscription()

        probe.requestNext(Delta(add = Map(nodeInfoA1.id -> nodeInfoA1, nodeInfoB1.id -> nodeInfoB1)))

        deltaStream.del(nodeInfoA2)
        probe.requestNext(Delta(del = Map(nodeInfoA2.id -> nodeInfoA2)))

        deltaStream.add(nodeInfoC1)
        probe.requestNext(Delta(add = Map(nodeInfoC1.id -> nodeInfoC1)))
    }

}
