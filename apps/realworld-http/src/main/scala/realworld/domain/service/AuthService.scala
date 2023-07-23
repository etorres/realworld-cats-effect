package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Email
import realworld.domain.model.User.Token
import realworld.shared.application.SecurityConfig
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.std.UUIDGen
import cats.effect.{Clock, IO}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import java.time.Duration

trait AuthService:
  def tokenFor(email: Email): IO[Token]

  def verify(token: Token): IO[Email]

object AuthService:
  def impl(
      securityConfig: SecurityConfig,
  )(using clock: Clock[IO], uuidGen: UUIDGen[IO]): AuthService = new AuthService:
    import scala.language.unsafeNulls

    private inline def issuer = securityConfig.issuer
    private inline val claimName = "userEmail"

    private inline def algorithm = Algorithm.HMAC256(securityConfig.secret.value)
    private inline def verifier = JWT.require(algorithm).withIssuer(issuer).build()

    override def tokenFor(email: Email): IO[Token] =
      for
        now <- clock.realTimeInstant
        uuid <- uuidGen.randomUUID
        jwtToken = JWT
          .create()
          .withIssuer(issuer)
          .withClaim(claimName, email)
          .withIssuedAt(now)
          .withExpiresAt(now.plus(Duration.ofHours(1L)))
          .withJWTId(uuid.toString)
          .sign(algorithm)
        token <- Token.from(jwtToken).validated
      yield token

    override def verify(token: Token): IO[Email] = for
      decodedJwt <- IO.delay(verifier.verify(token.value.value))
      claim = decodedJwt.getClaim(claimName).asString
      email <- Email.from(claim).validated
    yield email
