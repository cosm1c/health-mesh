package prowse.akka

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.google.protobuf.CodedInputStream
import spray.json.{DefaultJsonProtocol, pimpString}

case class ExampleModel(message: String)

object ExampleJsonProtobufCodec {

    import DefaultJsonProtocol._

    implicit val jsonFormat = DefaultJsonProtocol.jsonFormat1(ExampleModel.apply)

    def marshallAsText(model: ExampleModel): String = jsonFormat.write(model).compactPrint

    def marshallAsBinary(model: ExampleModel): ByteString =
        ByteString(
            ExampleModelOuterClass.ExampleModel.newBuilder()
                .setMessage(model.message)
                .build
                .toByteArray
        )

    def unmarshallText(text: String): ExampleModel = jsonFormat.read(text.parseJson)

    def unmarshallBinary(data: ByteString): ExampleModel =
        ExampleModel(
            ExampleModelOuterClass.ExampleModel
                .parseFrom(CodedInputStream.newInstance(data.asByteBuffer))
                .getMessage
        )
}

object ExampleWebsocketFlow {

    def create(log: LoggingAdapter)(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {

        val inbound: Sink[Message, Any] = Sink.foreach {
            case bm: BinaryMessage =>
                bm.dataStream.runForeach(byteString => {
                    val model = ExampleJsonProtobufCodec.unmarshallBinary(byteString)
                    log.info("RCV bin: {}", model.message)
                })
                ()

            case tm: TextMessage =>
                tm.textStream.runForeach(text => {
                    val model = ExampleJsonProtobufCodec.unmarshallText(text)
                    log.info("RCV txt: {}", model.message)
                })
                ()
        }

        val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

        Flow.fromSinkAndSourceMat(inbound, outbound)((inboundMat, outboundMat) => {
            outboundMat.offer(TextMessage(ExampleJsonProtobufCodec.marshallAsText(ExampleModel("Text from server"))))
            outboundMat.offer(BinaryMessage(ExampleJsonProtobufCodec.marshallAsBinary(ExampleModel("Binary from server"))))
            NotUsed
        })
    }

}
