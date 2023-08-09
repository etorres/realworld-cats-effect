package es.eriktorr
package realworld.application

import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest, UpdateUserRequest}
import realworld.adapter.rest.response.{
  GetCurrentUserResponse,
  LoginUserResponse,
  RegisterNewUserResponse,
  UpdateUserResponse,
}
import realworld.application.HttpAppSuite.*
import realworld.application.HttpAppSuiteRunner.{runWith, HttpAppState}
import realworld.application.UsersRestControllerSuite.{
  successfulGetCurrentUserGen,
  successfulUpdateAnExistingUser,
  successfulUserLoginGen,
  successfulUserRegistrationGen,
}
import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.Username
import realworld.domain.model.UsersGenerators.*
import realworld.domain.model.{Email, User}

import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.headers.Authorization
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class UsersRestControllerSuite extends HttpAppSuite:
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
      given Decoder[UpdateUserResponse] = UpdateUserResponse.updatedUserResponseJsonDecoder
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

object UsersRestControllerSuite:
  private val successfulGetCurrentUserGen = for
    case selectedUser :: otherUsers <- uniqueUserData(7)
    allUsers = selectedUser :: otherUsers
    authorization = authorizationFor(selectedUser.userWithPassword.user)
    initialState = HttpAppState.empty
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.copy()
    request = Nil
    expectedResponse = (GetCurrentUserResponse(selectedUser.userWithPassword.user), Status.Ok)
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)

  private val successfulUserLoginGen = for
    case selectedUser :: otherUsers <- uniqueUserData(7)
    allUsers = selectedUser :: otherUsers
    initialState = HttpAppState.empty
      .setPasswords(passwordsFrom(allUsers))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
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
    userId <- userIdGen
    userWithPassword <- userWithHashPasswordGen(userGen = user, passwordGen = password)
    initialState = HttpAppState.empty
      .setPasswords(Map(password -> userWithPassword.password))
      .setUsersWithPassword(List(userId), Map.empty)
    expectedState = initialState.setUsersWithPassword(List.empty, Map(userId -> userWithPassword))
    request = RegisterNewUserRequest(
      RegisterNewUserRequest.User(user.email, password.value, user.username),
    )
    expectedResponse = (RegisterNewUserResponse(user), Status.Ok)
  yield TestCase(None, initialState, expectedState, request, expectedResponse)

  private val successfulUpdateAnExistingUser = for
    case selectedUser :: otherUsers <- uniqueUserData(7)
    allUsers = selectedUser :: otherUsers
    updatedPassword <- passwordGen.retryUntil(_ != selectedUser.password, 100)
    updatedUser <- userWithHashPasswordGen(userGen(tokenGen = None), passwordGen = updatedPassword)
      .retryUntil(_ != selectedUser.userWithPassword, 100)
    authorization = authorizationFor(selectedUser.userWithPassword.user)
    initialState = HttpAppState.empty
      .setPasswords(passwordsFrom(allUsers) + (updatedPassword -> updatedUser.password))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.setUsersWithPassword(
      List.empty,
      allUsers.map { case UserData(_, userId, userWithPassword) =>
        userId -> (if userId != selectedUser.userId then userWithPassword else updatedUser)
      }.toMap,
    )
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
