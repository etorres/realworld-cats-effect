package es.eriktorr
package realworld.users.core.domain

import realworld.application.SecurityConfig
import realworld.application.SecurityConfig.{JwtIssuer, JwtSecret}
import realworld.common.Secret
import realworld.users.core.domain.UsersGenerators.emailGen

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
