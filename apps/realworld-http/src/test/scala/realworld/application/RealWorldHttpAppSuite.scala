package es.eriktorr
package realworld.application

import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest, UpdateUserRequest}
import realworld.adapter.rest.response.{
  GetCurrentUserResponse,
  LoginUserResponse,
  RegisterNewUserResponse,
  UpdateUserResponse,
}
import realworld.application.RealWorldHttpAppSuite.{
  successfulGetCurrentUserGen,
  successfulUpdateAnExistingUser,
  successfulUserLoginGen,
  successfulUserRegistrationGen,
}
import realworld.application.RealWorldHttpAppSuiteRunner.{runWith, RealWorldHttpAppState}
import realworld.domain.model.Password.{CipherText, PlainText}
import realworld.domain.model.RealWorldGenerators.*
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Password, User}
import realworld.shared.spec.CollectionGenerators.nDistinct

import cats.implicits.toTraverseOps
import io.circe.Decoder
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.Credentials.Token
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.headers.Authorization
import org.http4s.implicits.uri
import org.http4s.{AuthScheme, Method, Request, Status}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

final class RealWorldHttpAppSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should get the current user"):
    forAllF(successfulGetCurrentUserGen): testCase =>
      given Decoder[GetCurrentUserResponse] =
        GetCurrentUserResponse.getCurrentUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.GET, uri = uri"/api/users").putHeaders(testCase.authorization),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

  test("should login a user"):
    forAllF(successfulUserLoginGen): testCase =>
      given Decoder[LoginUserResponse] = LoginUserResponse.loginUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.POST, uri = uri"/api/users/login").withEntity(testCase.request),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
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
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

  test("should update an existing user"):
    forAllF(successfulUpdateAnExistingUser): testCase =>
      given Decoder[UpdateUserResponse] = UpdateUserResponse.updateUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.PUT, uri = uri"/api/users")
            .putHeaders(testCase.authorization)
            .withEntity(testCase.request),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

object RealWorldHttpAppSuite:
  final private case class TestCase[A, B](
      authorization: Option[Authorization],
      initialState: RealWorldHttpAppState,
      expectedState: RealWorldHttpAppState,
      request: A,
      expectedResponse: (B, Status),
  )

  private val successfulGetCurrentUserGen = for
    emails <- nDistinct(7, emailGen)
    tokens <- nDistinct(7, tokenGen)
    case selectedUser :: otherUsers <- emails
      .zip(tokens)
      .traverse { case (email, token) =>
        for
          password <- passwordGen
          user <- userGen(email, Some(token))
          userWithPassword <- userWithHashPasswordGen(user, password)
        yield TestUser(password, userWithPassword)
      }
    allUsers = selectedUser :: otherUsers
    authorization = Authorization(
      Token(
        AuthScheme.Bearer,
        selectedUser.userWithPassword.user.token.map(_.value.value).getOrElse(""),
      ),
    )
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
    request = Nil
    expectedResponse = (GetCurrentUserResponse(selectedUser.userWithPassword.user), Status.Ok)
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)

  final private case class TestUser(
      password: Password[PlainText],
      userWithPassword: UserWithHashPassword,
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
          userWithPassword <- userWithHashPasswordGen(user, password)
        yield TestUser(password, userWithPassword)
      }
    allUsers = selectedUser :: otherUsers
    initialState = RealWorldHttpAppState.empty
      .setPasswords(allUsers.map { case TestUser(password, userWithPassword) =>
        password -> userWithPassword.password
      }.toMap)
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
  yield TestCase(None, initialState, expectedState, request, expectedResponse)

  private val successfulUserRegistrationGen = for
    password <- passwordGen
    user <- userGen(tokenGen = None, bioGen = None, imageGen = None)
    userWithPassword <- userWithHashPasswordGen(userGen = user, passwordGen = password)
    initialState = RealWorldHttpAppState.empty.setPasswords(
      Map(password -> userWithPassword.password),
    )
    expectedState = initialState.setUsersWithPassword(List(userWithPassword))
    request = RegisterNewUserRequest(
      RegisterNewUserRequest.User(user.email, password.value, user.username),
    )
    expectedResponse = (RegisterNewUserResponse(user), Status.Ok)
  yield TestCase(None, initialState, expectedState, request, expectedResponse)

  private val successfulUpdateAnExistingUser = for
    emails <- nDistinct(7, emailGen)
    tokens <- nDistinct(7, tokenGen)
    case selectedUser :: otherUsers <- emails
      .zip(tokens)
      .traverse { case (email, token) =>
        for
          password <- passwordGen
          user <- userGen(email, Some(token))
          userWithPassword <- userWithHashPasswordGen(user, password)
        yield TestUser(password, userWithPassword)
      }
    allUsers = selectedUser :: otherUsers
    updatedPassword <- passwordGen.retryUntil(_ != selectedUser.password, 100)
    updatedUser <- userWithHashPasswordGen(
      userGen(emailGen = selectedUser.userWithPassword.user.email, tokenGen = None),
      passwordGen = updatedPassword,
    ).retryUntil(_ != selectedUser.userWithPassword, 100)
    authorization = Authorization(
      Token(
        AuthScheme.Bearer,
        selectedUser.userWithPassword.user.token.map(_.value.value).getOrElse(""),
      ),
    )
    initialState = RealWorldHttpAppState.empty
      .setPasswords(allUsers.map { case TestUser(password, userWithPassword) =>
        password -> userWithPassword.password
      }.toMap + (updatedPassword -> updatedUser.password))
      .setTokens(
        allUsers
          .map { case TestUser(_, userWithPassword) =>
            userWithPassword.user.email -> userWithPassword.user.token
          }
          .collect { case (email, Some(token)) => email -> token }
          .toMap,
      )
      .setUsersWithPassword(allUsers.map { case TestUser(_, userWithPassword) => userWithPassword })
    expectedState = initialState.setUsersWithPassword(allUsers.map {
      case TestUser(_, userWithPassword) =>
        if userWithPassword.user.email != updatedUser.user.email then userWithPassword
        else updatedUser
    })
    request = UpdateUserRequest(
      UpdateUserRequest
        .User(
          updatedUser.user.email,
          updatedPassword.value,
          updatedUser.user.username,
          updatedUser.user.bio,
          updatedUser.user.image.map(_.toString),
        ),
    )
    expectedResponse = (UpdateUserResponse(updatedUser.user), Status.Ok)
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)
