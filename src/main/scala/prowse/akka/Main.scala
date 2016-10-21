package prowse.akka

import java.lang.Thread.UncaughtExceptionHandler
import java.util.logging.{Level, LogManager}

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.bridge.SLF4JBridgeHandler

object Main extends LazyLogging {

    // Redirect all logging calls to SLF4J
    LogManager.getLogManager.reset()
    SLF4JBridgeHandler.install()
    java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST)

    def main(args: Array[String]): Unit = {
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler {
            override def uncaughtException(t: Thread, e: Throwable): Unit = {
                logger.error("UncaughtException on main thread", e)
            }
        })

        akka.Main.main(Array(classOf[AppSupervisorActor].getName))
    }

}
