package es.eriktorr
package realworld.application

import realworld.application.HttpAppSuiteRunner.HttpAppState
import realworld.domain.model.Password.PlainText
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Password, UserId}

import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.Status
import org.http4s.headers.Authorization

trait HttpAppSuite extends CatsEffectSuite with ScalaCheckEffectSuite

object HttpAppSuite:
  final case class TestCase[A, B](
      authorization: Option[Authorization],
      initialState: HttpAppState,
      expectedState: HttpAppState,
      request: A,
      expectedResponse: (B, Status),
  )

  final case class UserData(
      password: Password[PlainText],
      userId: UserId,
      userWithPassword: UserWithHashPassword,
  )
