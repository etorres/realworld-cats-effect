package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Email
import realworld.domain.service.AuthService.Token
import realworld.shared.Secret
import realworld.shared.application.SecurityConfig
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.std.UUIDGen
import cats.effect.{Clock, IO}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import java.time.Duration

trait AuthService:
  def tokenFor(email: Email): IO[Secret[Token]]

  def verify(token: Secret[Token]): IO[Email]

object AuthService:
  opaque type Token <: String = String
  object Token:
    def apply(value: String): Token = value

  def impl(
      securityConfig: SecurityConfig,
  )(using clock: Clock[IO], uuidGen: UUIDGen[IO]): AuthService = new AuthService:
    import scala.language.unsafeNulls

    private inline def issuer = securityConfig.issuer
    private inline val claimName = "userEmail"

    private inline def algorithm = Algorithm.HMAC256(securityConfig.secret.value)
    private inline def verifier = JWT.require(algorithm).withIssuer(issuer).build()

    override def tokenFor(email: Email): IO[Secret[Token]] =
      for
        now <- clock.realTimeInstant
        uuid <- uuidGen.randomUUID
        token = JWT
          .create()
          .withIssuer(issuer)
          .withClaim(claimName, email)
          .withIssuedAt(now)
          .withExpiresAt(now.plus(Duration.ofHours(1L)))
          .withJWTId(uuid.toString)
          .sign(algorithm)
      yield Secret(Token(token))

    override def verify(token: Secret[Token]): IO[Email] = for
      decodedJwt <- IO.delay(verifier.verify(token.value))
      claim = decodedJwt.getClaim(claimName).toString
      email <- Email.from(claim).validated
    yield email
