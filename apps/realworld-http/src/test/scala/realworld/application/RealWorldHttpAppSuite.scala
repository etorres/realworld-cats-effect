package es.eriktorr
package realworld.application

import realworld.adapter.rest.request.UserLoginRequest
import realworld.adapter.rest.response.UserLoginResponse
import realworld.application.RealWorldHttpAppSuite.successfulUserLoginGen
import realworld.application.RealWorldHttpAppSuiteRunner.{runWith, RealWorldHttpAppState}
import realworld.domain.model.Password.ClearText
import realworld.domain.model.RealWorldGenerators.*
import realworld.domain.model.{Password, User, UserWithPassword}
import realworld.shared.Secret
import realworld.shared.spec.CollectionGenerators.nDistinct

import cats.implicits.toTraverseOps
import io.circe.Decoder
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.*
import org.scalacheck.effect.PropF.forAllF

import scala.collection.immutable.::

final class RealWorldHttpAppSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should login a user"):
    forAllF(successfulUserLoginGen): testCase =>
      given Decoder[UserLoginResponse] = UserLoginResponse.userLoginResponseDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.POST, uri = uri"/api/user/login").withEntity(testCase.request),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(finalState, testCase.expectedState)
        assertEquals(result, Right(testCase.expectedResponse))
      }

object RealWorldHttpAppSuite:
  final private case class TestCase[A, B](
      initialState: RealWorldHttpAppState,
      expectedState: RealWorldHttpAppState,
      request: A,
      expectedResponse: (B, Status),
  )

  final private case class TestUser(
      password: Password[ClearText],
      userWithPassword: UserWithPassword,
  )

  private val successfulUserLoginGen = for
    emails <- nDistinct(7, emailGen)
    tokens <- nDistinct(7, tokenGen)
    case selectedUser :: otherUsers <- emails
      .zip(tokens)
      .traverse { case (email, token) =>
        for
          password <- passwordGen
          userWithPassword <- userWithPasswordGen(userGen(email, Some(token)), password)
        yield TestUser(password, userWithPassword)
      }
    allUsers = selectedUser :: otherUsers
    initialState = RealWorldHttpAppState.empty
      .setTokens(
        allUsers
          .map { case TestUser(_, userWithPassword) =>
            userWithPassword.user.email -> userWithPassword.user.token
          }
          .collect { case (email, Some(token)) => email -> token }
          .toMap,
      )
      .setUsersWithPassword(allUsers.map { case TestUser(_, userWithPassword) =>
        userWithPassword.user.email -> userWithPassword
      }.toMap)
    expectedState = initialState.copy()
    request = UserLoginRequest(
      UserLoginRequest.User(
        selectedUser.userWithPassword.user.email,
        Secret(selectedUser.password.toString),
      ),
    )
    expectedResponse = (UserLoginResponse(selectedUser.userWithPassword.user), Status.Ok)
  yield TestCase(initialState, expectedState, request, expectedResponse)
