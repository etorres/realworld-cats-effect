package es.eriktorr
package realworld.application

import realworld.adapter.rest.response.GetProfileResponse
import realworld.application.HttpAppSuite.*
import realworld.application.HttpAppSuiteRunner.{runWith, HttpAppState}
import realworld.application.ProfileRestControllerSuite.successfulGetProfileGen
import realworld.domain.model.Profile
import realworld.domain.model.UsersGenerators.*

import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.headers.Authorization
import org.http4s.{Method, Request, Status, Uri}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class ProfileRestControllerSuite extends HttpAppSuite:
  test("should get a user's profile"):
    forAllF(successfulGetProfileGen): testCase =>
      given Decoder[GetProfileResponse] = GetProfileResponse.getProfileResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(
            method = Method.GET,
            uri = Uri.unsafeFromString(s"/api/profiles/${testCase.request}"),
          ).putHeaders(testCase.authorization),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

object ProfileRestControllerSuite:
  private val successfulGetProfileGen = for
    case followed :: follower :: otherUsers <- uniqueUserData(7)
    allUsers = followed :: follower :: otherUsers
    following <- Gen.oneOf(true, false)
    followers = Map(
      followed.userId -> ((if following then List(follower.userId)
                           else List.empty) ++ otherUsers.map(_.userId)),
    )
    authorization = authorizationFor(follower.userWithPassword.user)
    initialState = HttpAppState.empty
      .setFollowers(followers)
      .setPasswords(passwordsFrom(allUsers))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.copy()
    request = followed.userWithPassword.user.username
    expectedResponse = (
      GetProfileResponse(
        Profile(
          followed.userWithPassword.user.username,
          followed.userWithPassword.user.bio,
          followed.userWithPassword.user.image,
          following,
        ),
      ),
      Status.Ok,
    )
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)
