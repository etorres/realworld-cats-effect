package es.eriktorr
package realworld.application

import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest}
import realworld.adapter.rest.response.{LoginUserResponse, RegisterNewUserResponse}
import realworld.application.RealWorldHttpAppSuite.{
  successfulUserLoginGen,
  successfulUserRegistrationGen,
}
import realworld.application.RealWorldHttpAppSuiteRunner.{runWith, RealWorldHttpAppState}
import realworld.domain.model.Password.ClearText
import realworld.domain.model.RealWorldGenerators.*
import realworld.domain.model.{Password, User, UserWithPassword}
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo
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

final class RealWorldHttpAppSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should login a user"):
    forAllF(successfulUserLoginGen): testCase =>
      given Decoder[LoginUserResponse] = LoginUserResponse.loginUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.POST, uri = uri"/api/users/login").withEntity(testCase.request),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(finalState, testCase.expectedState)
        assertEquals(result, Right(testCase.expectedResponse))
      }

  test("should register a new user"):
    forAllF(successfulUserRegistrationGen): testCase =>
      given Decoder[RegisterNewUserResponse] =
        RegisterNewUserResponse.registerNewUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.POST, uri = uri"/api/users").withEntity(testCase.request),
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
          user <- userGen(email, Some(token))
          userWithPassword <- userWithPasswordGen(user, password)
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
      .setUsersWithPassword(allUsers.map { case TestUser(_, userWithPassword) => userWithPassword })
    expectedState = initialState.copy()
    request = LoginUserRequest(
      LoginUserRequest.User(
        selectedUser.userWithPassword.user.email,
        selectedUser.password.value,
      ),
    )
    expectedResponse = (LoginUserResponse(selectedUser.userWithPassword.user), Status.Ok)
  yield TestCase(initialState, expectedState, request, expectedResponse)

  private val successfulUserRegistrationGen = for
    email <- emailGen
    password <- passwordGen
    username <- usernameGen
    user = User(email, None, username, None, None)
    userWithPassword = UserWithPassword(user, Password.cipher(password).orFail)
    initialState = RealWorldHttpAppState.empty
    expectedState = initialState.setUsersWithPassword(List(userWithPassword))
    request = RegisterNewUserRequest(RegisterNewUserRequest.User(email, password.value, username))
    expectedResponse = (RegisterNewUserResponse(user), Status.Ok)
  yield TestCase(initialState, expectedState, request, expectedResponse)
