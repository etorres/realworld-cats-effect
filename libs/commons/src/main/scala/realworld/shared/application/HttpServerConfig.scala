package es.eriktorr
package realworld.shared.application

import realworld.shared.application.HttpServerConfig.MaxActiveRequests
import realworld.shared.application.argument.IpArgument
import realworld.shared.refined.Types.Between

import cats.Show
import cats.data.ValidatedNel
import com.comcast.ip4s.{ipv4, port, Host, Port}
import com.monovore.decline.Argument
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

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

  given maxActiveRequestsArgument: Argument[MaxActiveRequests] = new Argument[MaxActiveRequests]:
    override def read(string: String): ValidatedNel[String, MaxActiveRequests] =
      string.toLong.refineValidatedNel[Between[1L, 4096L]].map(MaxActiveRequests.apply)

    override def defaultMetavar: String = "limit"

  export IpArgument.given
