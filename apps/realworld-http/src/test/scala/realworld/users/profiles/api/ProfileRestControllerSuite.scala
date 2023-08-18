package es.eriktorr
package realworld.users.profiles.api

import realworld.common.api.HttpAppSuite
import realworld.common.api.HttpAppSuite.*
import realworld.common.api.HttpAppSuiteRunner.{runWith, HttpAppState}
import realworld.users.core.domain.UsersGenerators.uniqueUserData
import realworld.users.profiles.api.ProfileRestControllerSuite.{
  successfulFollowUserGen,
  successfulGetProfileGen,
  successfulUnfollowUserGen,
}
import realworld.users.profiles.api.response.{
  FollowUserResponse,
  GetProfileResponse,
  UnfollowUserResponse,
}
import realworld.users.profiles.domain.Profile

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

  test("should follow a user"):
    forAllF(successfulFollowUserGen): testCase =>
      given Decoder[FollowUserResponse] = FollowUserResponse.followUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(
            method = Method.POST,
            uri = Uri.unsafeFromString(s"/api/profiles/${testCase.request}/follow"),
          ).putHeaders(testCase.authorization),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

  test("should unfollow a user"):
    forAllF(successfulUnfollowUserGen): testCase =>
      given Decoder[UnfollowUserResponse] = UnfollowUserResponse.unfollowUserResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(
            method = Method.DELETE,
            uri = Uri.unsafeFromString(s"/api/profiles/${testCase.request}/follow"),
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
    viewer <- Gen.frequency(1 -> Gen.some(follower), 1 -> None)
    following <- viewer match
      case Some(_) => Gen.oneOf(true, false)
      case None => Gen.const(false)
    followers = Map(
      followed.userId -> ((if following then List(follower.userId)
                           else List.empty) ++ otherUsers.map(_.userId)),
    )
    authorization = viewer.map(x => authorizationFor(x.userWithPassword.user))
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
  yield TestCase(authorization, initialState, expectedState, request, expectedResponse)

  private val successfulFollowUserGen = for
    case followed :: follower :: otherUsers <- uniqueUserData(7)
    allUsers = followed :: follower :: otherUsers
    alreadyFollowing <- Gen.oneOf(true, false)
    followers = Map(
      followed.userId -> ((if alreadyFollowing then List(follower.userId)
                           else List.empty) ++ otherUsers.map(_.userId)),
    )
    authorization = authorizationFor(follower.userWithPassword.user)
    initialState = HttpAppState.empty
      .setFollowers(followers)
      .setPasswords(passwordsFrom(allUsers))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.setFollowers(
      Map(followed.userId -> (follower :: otherUsers).map(_.userId)),
    )
    request = followed.userWithPassword.user.username
    expectedResponse = (
      FollowUserResponse(
        Profile(
          followed.userWithPassword.user.username,
          followed.userWithPassword.user.bio,
          followed.userWithPassword.user.image,
          true,
        ),
      ),
      Status.Ok,
    )
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)

  private val successfulUnfollowUserGen = for
    case followed :: follower :: otherUsers <- uniqueUserData(7)
    allUsers = followed :: follower :: otherUsers
    alreadyFollowing <- Gen.oneOf(true, false)
    followers = Map(
      followed.userId -> ((if alreadyFollowing then List(follower.userId)
                           else List.empty) ++ otherUsers.map(_.userId)),
    )
    authorization = authorizationFor(follower.userWithPassword.user)
    initialState = HttpAppState.empty
      .setFollowers(followers)
      .setPasswords(passwordsFrom(allUsers))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.setFollowers(
      Map(followed.userId -> otherUsers.map(_.userId)),
    )
    request = followed.userWithPassword.user.username
    expectedResponse = (
      UnfollowUserResponse(
        Profile(
          followed.userWithPassword.user.username,
          followed.userWithPassword.user.bio,
          followed.userWithPassword.user.image,
          false,
        ),
      ),
      Status.Ok,
    )
  yield TestCase(Some(authorization), initialState, expectedState, request, expectedResponse)
