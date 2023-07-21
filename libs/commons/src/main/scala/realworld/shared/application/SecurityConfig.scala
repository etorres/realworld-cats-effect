package es.eriktorr
package realworld.shared.application

import realworld.shared.Secret
import realworld.shared.application.SecurityConfig.{JwtIssuer, JwtSecret}
import realworld.shared.refined.Types.NonEmptyString

import cats.Show
import cats.data.ValidatedNel
import com.monovore.decline.Argument
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class SecurityConfig(issuer: JwtIssuer, secret: Secret[JwtSecret])

object SecurityConfig:
  opaque type JwtIssuer <: String :| NonEmptyString = String :| NonEmptyString
  object JwtIssuer:
    def apply(value: String :| NonEmptyString): JwtIssuer = value

  opaque type JwtSecret <: String :| NonEmptyString = String :| NonEmptyString
  object JwtSecret:
    def apply(value: String :| NonEmptyString): JwtSecret = value
    given Show[JwtSecret] = Show.fromToString

  given Show[SecurityConfig] =
    Show.show(config => s"jwt-issuer: ${config.issuer}, jwt-secret: ${config.secret}")

  given jwtSecretArgument: Argument[JwtSecret] = new Argument[JwtSecret]:
    override def read(string: String): ValidatedNel[String, JwtSecret] =
      string.refineValidatedNel[NonEmptyString].map(JwtSecret.apply)

    override def defaultMetavar: String = "secret"

  given jwtIssuerArgument: Argument[JwtIssuer] = new Argument[JwtIssuer]:
    override def read(string: String): ValidatedNel[String, JwtIssuer] =
      string.refineValidatedNel[NonEmptyString].map(JwtIssuer.apply)

    override def defaultMetavar: String = "subject"
