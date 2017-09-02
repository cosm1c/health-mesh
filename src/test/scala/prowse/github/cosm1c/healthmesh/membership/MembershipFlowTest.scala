package prowse.github.cosm1c.healthmesh.membership

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import prowse.github.cosm1c.healthmesh.agentpool.ExampleAgent.{ExampleAgentUpdate, HealthStatus}
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow.{MemberId, MembershipCommand, MembershipDelta}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MembershipFlowTest extends FlatSpec with MockitoSugar {

    private implicit val actorSystem: ActorSystem = ActorSystem()
    private implicit val materializer: ActorMaterializer = ActorMaterializer()

    private def createPubSub(): (TestPublisher.Probe[MembershipCommand[ExampleAgentUpdate]], TestSubscriber.Probe[MembershipDelta[ExampleAgentUpdate]]) = {
        val (sink, srcGen): (Sink[MembershipCommand[ExampleAgentUpdate], NotUsed], () => Future[Source[MembershipDelta[ExampleAgentUpdate], NotUsed]]) =
            MembershipFlow.membershipFlow()

        val pub: TestPublisher.Probe[MembershipCommand[ExampleAgentUpdate]] =
            TestSource.probe[MembershipCommand[ExampleAgentUpdate]].toMat(sink)(Keep.left).run()
        pub.ensureSubscription()

        val eventualSource = srcGen()
        Await.ready(eventualSource, 1.second)
        val source = eventualSource.value.get.get

        val sub: TestSubscriber.Probe[MembershipDelta[ExampleAgentUpdate]] =
            source.toMat(TestSink.probe[MembershipDelta[ExampleAgentUpdate]])(Keep.right).run()
        sub.ensureSubscription()

        (pub, sub)
    }

    "MembershipFlow" should "not provide any data until first message" in {
        val (pub, sub) = createPubSub()

        sub.request(1L)
        sub.expectNoMsg()

        pub.sendNext(MembershipCommand(Map.empty[MemberId, ExampleAgentUpdate], Set.empty[MemberId]))
        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))
    }

    it should "add an item" in {
        val (pub, sub) = createPubSub()

        val addedMembers = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown))
        pub.sendNext(MembershipCommand(addedMembers))

        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "add multiple items" in {
        val (pub, sub) = createPubSub()

        val addedMembers = Map(
            "a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown),
            "b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy)
        )
        pub.sendNext(MembershipCommand(addedMembers, Set.empty))

        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))
    }

    it should "add two items" in {
        val (pub, sub) = createPubSub()

        val addedMembers1st = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown))
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers1st, addedMembers1st, Map.empty, Set.empty))

        val addedMembers2nd = Map("b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy))
        pub.sendNext(MembershipCommand(addedMembers2nd, Set.empty))

        val allMembers = addedMembers1st ++ addedMembers2nd
        assert(sub.requestNext() == MembershipDelta(allMembers, addedMembers2nd, Map.empty, Set.empty))
    }

    it should "update an item" in {
        val (pub, sub) = createPubSub()

        val addedMembers1st = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown))
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers1st, addedMembers1st, Map.empty, Set.empty))

        val addedMembers2nd = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Healthy))
        pub.sendNext(MembershipCommand(addedMembers2nd, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers2nd, Map.empty, addedMembers2nd, Set.empty))
    }

    it should "remove an item" in {
        val (pub, sub) = createPubSub()

        val addedMembers1st = Map(
            "a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown),
            "b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy)
        )
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers1st, addedMembers1st, Map.empty, Set.empty))

        val removedMembers2nd = Set("a")
        pub.sendNext(MembershipCommand(Map.empty, removedMembers2nd))
        assert(sub.requestNext() == MembershipDelta(Map("b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy)), Map.empty, Map.empty, removedMembers2nd))
    }

    it should "conflate updates" in {
        val (pub, sub) = createPubSub()

        val aPresent = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown))
        val bothPresent = Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown), "b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy))
        val setOfA = Set("A")

        pub.sendNext(MembershipCommand(bothPresent, Set.empty))
        pub.sendNext(MembershipCommand(Map.empty, setOfA))
        pub.sendNext(MembershipCommand(aPresent, Set.empty))
        assert(sub.requestNext() == MembershipDelta(bothPresent, bothPresent, Map.empty, Set.empty))
    }

    ignore should "drop removes for unknown items" in {
        val (pub, sub) = createPubSub()

        pub.sendNext(MembershipCommand(Map.empty[MemberId, ExampleAgentUpdate], Set.empty[MemberId]))
        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    ignore should "drop deletes that make no change" in {
        val (pub, sub) = createPubSub()

        pub.sendNext(MembershipCommand(Map.empty[MemberId, ExampleAgentUpdate], Set.empty[MemberId]))
        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    ignore should "drop delta when conflated updates make not change" in {
        val (pub, sub) = createPubSub()

        pub.sendNext(MembershipCommand(Map.empty[MemberId, ExampleAgentUpdate], Set.empty[MemberId]))
        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map(
            "a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown),
            "b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy)), Set.empty))
        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        pub.sendNext(MembershipCommand(Map.empty, Set("B")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    ignore should "drop updates that make no change" in {
        val (pub, sub) = createPubSub()

        val addedMembers = Map(
            "a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown),
            "b" -> ExampleAgentUpdate("b", Seq(), HealthStatus.Unhealthy))
        pub.sendNext(MembershipCommand(addedMembers, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map("a" -> ExampleAgentUpdate("a", Seq(), HealthStatus.Unknown)), Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }
}
