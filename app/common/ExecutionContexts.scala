package common

import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.ExecutionContext

object ExecutionContexts {
  implicit val fastOps: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val internetIOOps: ExecutionContext = Akka.system.dispatchers.lookup("contexts.internet-io-ops")
  implicit val verySlowOps: ExecutionContext = Akka.system.dispatchers.lookup("contexts.very-slow-ops")
}
