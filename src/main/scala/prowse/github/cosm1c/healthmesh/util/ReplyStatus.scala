package prowse.github.cosm1c.healthmesh.util

object ReplyStatus {

    sealed trait Status extends Serializable

    @SerialVersionUID(1L)
    final case object Success extends Status

    @SerialVersionUID(1L)
    final case object Failure extends Status

    //    @SerialVersionUID(1L)
    //    final case class Fatal(cause: Throwable) extends Status

}
