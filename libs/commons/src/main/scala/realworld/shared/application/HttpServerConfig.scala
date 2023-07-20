package es.eriktorr
package realworld.shared.application

import realworld.shared.application.HttpServerConfig.MaxActiveRequests
import realworld.shared.refined.Types.Between

import cats.Show
import com.comcast.ip4s.{ipv4, port, Host, Port}
import io.github.iltotore.iron.*

import scala.concurrent.duration.{DurationInt, FiniteDuration}

final case class HttpServerConfig(
    host: Host,
    maxActiveRequests: MaxActiveRequests,
    port: Port,
    timeout: FiniteDuration,
)

object HttpServerConfig:
  opaque type MaxActiveRequests <: Long :| Between[1L, 4096L] = Long :| Between[1L, 4096L]
  object MaxActiveRequests:
    def apply(value: Long :| Between[1L, 4096L]): MaxActiveRequests = value

  val defaultHost: Host = ipv4"0.0.0.0"
  val defaultMaxActiveRequests: MaxActiveRequests = MaxActiveRequests.apply(128L.refine)
  val defaultPort: Port = port"8080"
  val defaultTimeout: FiniteDuration = 10.seconds

  val default: HttpServerConfig =
    HttpServerConfig(defaultHost, defaultMaxActiveRequests, defaultPort, defaultTimeout)

  given Show[HttpServerConfig] =
    import scala.language.unsafeNulls
    Show.show(config => s"""http-host: ${config.host},
                           | http-max-active-requests: ${config.maxActiveRequests},
                           | http-port: ${config.port},
                           | http-timeout: ${config.timeout}""".stripMargin.replaceAll("\\R", ""))
