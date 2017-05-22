package prowse.github.cosm1c.healthmesh.membership

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import prowse.github.cosm1c.healthmesh.membership.MembershipFlow.{MembershipCommand, _}

class MembershipFlowTest extends FlatSpec with MockitoSugar {

    private implicit val actorSystem = ActorSystem()
    private implicit val materializer = ActorMaterializer()

    "MembershipFlow" should "provide a single empty state before any commands received" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "add an item" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers = Map("A" -> "aaa")
        pub.sendNext(MembershipCommand(addedMembers, Set.empty))

        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "add multiple items" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers = Map("A" -> "aaa", "B" -> "bbb")
        pub.sendNext(MembershipCommand(addedMembers, Set.empty))

        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))
    }

    it should "add an item twice" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers1st = Map("A" -> "aaa")
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        val addedMembers2nd = Map("B" -> "bbb")
        pub.sendNext(MembershipCommand(addedMembers2nd, Set.empty))

        val allMembers = addedMembers1st ++ addedMembers2nd
        assert(sub.requestNext() == MembershipDelta(allMembers, allMembers, Map.empty, Set.empty))
    }

    it should "update an item" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers1st = Map("A" -> "aaa")
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers1st, addedMembers1st, Map.empty, Set.empty))

        val addedMembers2nd = Map("A" -> "AAA")
        pub.sendNext(MembershipCommand(addedMembers2nd, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers2nd, Map.empty, addedMembers2nd, Set.empty))
    }

    it should "remove an item" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers1st = Map("A" -> "aaa", "B" -> "bbb")
        pub.sendNext(MembershipCommand(addedMembers1st, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers1st, addedMembers1st, Map.empty, Set.empty))

        val removedMembers2nd = Set("A")
        pub.sendNext(MembershipCommand(Map.empty, removedMembers2nd))
        assert(sub.requestNext() == MembershipDelta(Map("B" -> "bbb"), Map.empty, Map.empty, removedMembers2nd))
    }

    it should "conflate updates" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val aPresent = Map("A" -> "aaa")
        val bothPresent = Map("A" -> "aaa", "B" -> "bbb")
        val setOfA = Set("A")

        pub.sendNext(MembershipCommand(bothPresent, Set.empty))
        pub.sendNext(MembershipCommand(Map.empty, setOfA))
        pub.sendNext(MembershipCommand(aPresent, Set.empty))
        assert(sub.requestNext() == MembershipDelta(bothPresent, bothPresent, Map.empty, Set.empty))
    }

    it should "conflate updates additionally" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val justA = Map("A" -> "aaa")
        val bothBandC = Map("B" -> "bbb", "c" -> "ccc")
        val bothDandE = Map("D" -> "ddd", "E" -> "eee")
        val all = justA ++ bothBandC ++ bothDandE

        pub.sendNext(MembershipCommand(bothBandC, Set.empty))
        pub.sendNext(MembershipCommand(justA, Set.empty))
        pub.sendNext(MembershipCommand(bothDandE, Set.empty))
        assert(sub.requestNext() == MembershipDelta(all, all, Map.empty, Set.empty))
    }

    it should "re-create objects that were deleted in earlier messages" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val aPresent = Map("A" -> "aaa")
        val bPresent = Map("B" -> "bbb")
        val bothPresent = Map("A" -> "aaa", "B" -> "bbb")
        val setOfA = Set("A")

        pub.sendNext(MembershipCommand(bothPresent, Set.empty))
        assert(sub.requestNext() == MembershipDelta(bothPresent, bothPresent, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map.empty, setOfA))
        assert(sub.requestNext() == MembershipDelta(bPresent, Map.empty, Map.empty, setOfA))

        pub.sendNext(MembershipCommand(aPresent, Set.empty))
        assert(sub.requestNext() == MembershipDelta(bothPresent, aPresent, Map.empty, Set.empty))
    }

    it should "apply upserts conflated previously as additions as added not updated" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()


        pub.sendNext(MembershipCommand(Map("A" -> "aaa", "B" -> "bbb"), Set.empty))
        pub.sendNext(MembershipCommand(Map("A" -> "AAA"), Set.empty))
        pub.sendNext(MembershipCommand(Map("B" -> "BBB"), Set.empty))


        val expectedAdds = Map("A" -> "AAA", "B" -> "BBB")
        assert(sub.requestNext() == MembershipDelta(expectedAdds, expectedAdds, Map.empty, Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }

    // ensure item is not marked as remove in delta

    it should "drop removes for unknown items" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "drop deletes that make no change" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "drop delta when conflated updates make not change" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        assert(sub.requestNext() == MembershipDelta(Map.empty, Map.empty, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map("A" -> "aaa", "B" -> "bbb"), Set.empty))
        pub.sendNext(MembershipCommand(Map.empty, Set("A")))
        pub.sendNext(MembershipCommand(Map.empty, Set("B")))
        sub.request(1L)
        sub.expectNoMsg()
    }

    it should "drop updates that make no change" in {
        val (pub, sub) = TestSource.probe[MembershipCommand[String, String]]
            .via(new MembershipFlow[String, String])
            .toMat(TestSink.probe[MembershipDelta[String, String]])(Keep.both)
            .run()

        val addedMembers = Map("A" -> "aaa", "B" -> "bbb")
        pub.sendNext(MembershipCommand(addedMembers, Set.empty))
        assert(sub.requestNext() == MembershipDelta(addedMembers, addedMembers, Map.empty, Set.empty))

        pub.sendNext(MembershipCommand(Map("A" -> "aaa"), Set.empty))
        sub.request(1L)
        sub.expectNoMsg()
    }
}
