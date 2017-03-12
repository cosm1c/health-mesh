package prowse.github.cosm1c.healthmesh.util

import akka.Done

/**
  * Inspired by akka.actor.Status
  */
object Status {

    sealed trait Status[A] extends Serializable

    @SerialVersionUID(1L)
    final case class Success[A](status: A) extends Status[A]

    @SerialVersionUID(1L)
    final case class Failure[A](cause: Throwable) extends Status[A]

    val SUCCESS_DONE: Status[Done] = Success(Done)
}
