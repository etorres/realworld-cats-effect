package es.eriktorr
package realworld.domain.service

import realworld.domain.model.UsersGenerators.emailGen
import realworld.shared.Secret
import realworld.shared.application.SecurityConfig
import realworld.shared.application.SecurityConfig.{JwtIssuer, JwtSecret}

import cats.effect.{IO, Resource}
import io.github.iltotore.iron.autoRefine
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF

final class AuthServiceSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  private val securityConfig =
    SecurityConfig(JwtIssuer("jwt-issuer"), Secret(JwtSecret("jwt-secret")))

  val authServiceFixture: Fixture[AuthService] =
    ResourceSuiteLocalFixture(
      "auth-service",
      Resource.eval(IO.delay(AuthService.impl(securityConfig))),
    )

  override def munitFixtures: Seq[Fixture[?]] = List(authServiceFixture)

  test("generated tokens are verifiable"):
    forAllF(emailGen): email =>
      val authService = authServiceFixture()
      for
        token <- authService.tokenFor(email)
        _ <- authService.verify(token).assertEquals(email)
      yield ()
