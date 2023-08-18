package es.eriktorr
package realworld.application

import realworld.application.JdbcConfig.{ConnectUrl, DriverClassName, Password, Username}
import realworld.application.argument.RangeArgument
import realworld.common.Secret
import realworld.common.data.refined.Constraints.{JdbcUrl, NonEmptyString}

import cats.Show
import cats.collections.Range
import cats.data.ValidatedNel
import com.monovore.decline.Argument
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class JdbcConfig(
    connections: Range[Int],
    connectUrl: ConnectUrl,
    driverClassName: DriverClassName,
    password: Secret[Password],
    username: Username,
)

object JdbcConfig:
  opaque type ConnectUrl <: String :| JdbcUrl = String :| JdbcUrl
  object ConnectUrl:
    def apply(value: String :| JdbcUrl): ConnectUrl = value

  opaque type DriverClassName = String :| NonEmptyString
  object DriverClassName extends RefinedTypeOps[DriverClassName]

  opaque type Password <: String :| NonEmptyString = String :| NonEmptyString
  object Password:
    def apply(value: String :| NonEmptyString): Password = value
    given Show[Password] = Show.fromToString

  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString
  object Username:
    def apply(value: String :| NonEmptyString): Username = value

  def postgresql(
      connections: Range[Int],
      connectUrl: ConnectUrl,
      password: Secret[Password],
      username: Username,
  ): JdbcConfig = JdbcConfig(
    connections,
    connectUrl,
    DriverClassName.applyUnsafe("org.postgresql.Driver "),
    password,
    username,
  )

  given Show[JdbcConfig] =
    import scala.language.unsafeNulls
    Show.show(config => s"""jdbc-connections: ${config.connections.start}-${config.connections.end},
                           | jdbc-connect-url: ${config.connectUrl},
                           | jdbc-driver-class-name: ${config.driverClassName},
                           | jdbc-password: ${config.password},
                           | jdbc-username: ${config.username}""".stripMargin.replaceAll("\\R", ""))

  given connectUrlArgument: Argument[ConnectUrl] = new Argument[ConnectUrl]:
    override def read(string: String): ValidatedNel[String, ConnectUrl] =
      string.refineValidatedNel[JdbcUrl].map(ConnectUrl.apply)

    override def defaultMetavar: String = "url"

  given passwordArgument: Argument[Password] = new Argument[Password]:
    override def read(string: String): ValidatedNel[String, Password] =
      string.refineValidatedNel[NonEmptyString].map(Password.apply)

    override def defaultMetavar: String = "password"

  given usernameArgument: Argument[Username] = new Argument[Username]:
    override def read(string: String): ValidatedNel[String, Username] =
      string.refineValidatedNel[NonEmptyString].map(Username.apply)

    override def defaultMetavar: String = "username"

  export RangeArgument.given
