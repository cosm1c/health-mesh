package prowse.github.cosm1c.healthmesh.faststart

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class FastStartBroadcastActorTest extends TestKit(ActorSystem("MySpec")) with FlatSpecLike with BeforeAndAfterAll with MockitoSugar {

    private implicit val materializer: ActorMaterializer = ActorMaterializer()

    override def afterAll: Unit = {
        TestKit.shutdownActorSystem(system)
    }

    private def runWithProbes(): (TestPublisher.Probe[Int], () => Future[Source[String, NotUsed]]) = {
        val (dataSink, srcGenerator): (Sink[Int, NotUsed], () => Future[Source[String, NotUsed]]) =
            FastStartBroadcast.dataSinkAndDataSourceGenerator[Int, String](String.valueOf, (i, str) => str + i, (_, b) => b)

        val pub = TestSource.probe[Int].toMat(dataSink)(Keep.left).run()
        pub.ensureSubscription()

        (pub, srcGenerator)
    }

    private def createSubscriber(srcGenerator: () => Future[Source[String, NotUsed]]): Probe[String] = {
        val futureSource = srcGenerator()
        Await.ready(futureSource, 1.second)
        futureSource.value.get.get
            .toMat(TestSink.probe[String])(Keep.right)
            .run()
    }

    "FastStartBroadcastActor" should "pass through" in {
        val (pub, src) = runWithProbes()
        val sub = createSubscriber(src)

        pub.sendNext(1)
        sub.requestNext("1")
    }

    it should "allow subscribers before first data element" in {
        val (pub, src) = runWithProbes()
        val sub = createSubscriber(src)

        sub.request(1L)
        sub.expectNoMsg()
        pub.sendNext(1)
        sub.expectNext("1")
    }

    it should "send last element for new subscribers" in {
        val (pub, src) = runWithProbes()

        val subA = createSubscriber(src)
        subA.ensureSubscription()

        pub.sendNext(1)
        subA.requestNext("1")
        pub.sendNext(2)
        subA.requestNext("12")

        val subB = createSubscriber(src)
        subB.ensureSubscription()
        subB.requestNext("12")

        pub.sendNext(3)
        subA.requestNext("123")
        subB.requestNext("123")
    }

}
