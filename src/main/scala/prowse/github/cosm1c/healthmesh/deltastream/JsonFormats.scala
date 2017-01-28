package prowse.github.cosm1c.healthmesh.deltastream

import java.time.Instant
import java.util.concurrent.TimeUnit._

import spray.json
import spray.json.{JsNumber, JsValue, RootJsonFormat}

import scala.concurrent.duration.{Duration, FiniteDuration}

object JsonFormats {

    implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
        def write(instant: Instant) = JsNumber(instant.toEpochMilli)

        def read(value: JsValue): Instant = value match {
            case JsNumber(millis) => Instant.ofEpochMilli(millis.longValue())
            case _ => json.deserializationError("Expected JSNumber for Instant")
        }
    }

    implicit object DurationJsonFormat extends RootJsonFormat[Duration] {
        def write(duration: Duration) = JsNumber(duration.toNanos)

        def read(value: JsValue): FiniteDuration = value match {
            case JsNumber(nanos) => Duration.create(nanos.longValue, NANOSECONDS)
            case _ => json.deserializationError("Expected JSNumber for Duration")
        }
    }

}
