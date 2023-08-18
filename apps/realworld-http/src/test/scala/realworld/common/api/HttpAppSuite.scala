package es.eriktorr
package realworld.common.api

import realworld.common.api.HttpAppSuiteRunner.HttpAppState
import realworld.users.core.domain.Password.{CipherText, PlainText}
import realworld.users.core.domain.User.Token
import realworld.users.core.domain.UserWithPassword.UserWithHashPassword
import realworld.users.core.domain.UsersGenerators.UserData
import realworld.users.core.domain.{Email, Password, User, UserId}

import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.Credentials.Token as Http4sToken
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Status}

trait HttpAppSuite extends CatsEffectSuite with ScalaCheckEffectSuite

object HttpAppSuite:
  final case class TestCase[A, B](
      authorization: Option[Authorization],
      initialState: HttpAppState,
      expectedState: HttpAppState,
      request: A,
      expectedResponse: (B, Status),
  )

  def authorizationFor(user: User): Authorization = Authorization(
    Http4sToken(
      AuthScheme.Bearer,
      user.token.map(_.value.value).getOrElse("anonymous"),
    ),
  )

  def passwordsFrom(usersData: List[UserData]): Map[Password[PlainText], Password[CipherText]] =
    usersData.map { case UserData(password, _, userWithPassword) =>
      password -> userWithPassword.password
    }.toMap

  def tokensFrom(usersData: List[UserData]): Map[Email, Token] = usersData
    .map { case UserData(_, _, userWithPassword) =>
      userWithPassword.user.email -> userWithPassword.user.token
    }
    .collect { case (email, Some(token)) => email -> token }
    .toMap

  def usersWithPasswordFrom(usersData: List[UserData]): Map[UserId, UserWithHashPassword] =
    usersData.map { case UserData(_, userId, userWithPassword) =>
      userId -> userWithPassword
    }.toMap
