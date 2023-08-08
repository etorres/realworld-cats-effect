package es.eriktorr
package realworld.application

import realworld.adapter.rest.response.GetProfileResponse
import realworld.application.HttpAppSuite.{TestCase, UserData}
import realworld.application.HttpAppSuiteRunner.{runWith, HttpAppState}
import realworld.application.ProfileRestControllerSuite.successfulGetProfileGen
import realworld.domain.model.Profile
import realworld.domain.model.RealWorldGenerators.*
import realworld.shared.spec.CollectionGenerators.nDistinct

import cats.implicits.toTraverseOps
import io.circe.Decoder
import org.http4s.Credentials.Token
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Method, Request, Status, Uri}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
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
    userKeys <- uniqueUserKeys(7)
    tokens <- nDistinct(7, tokenGen)
    case followed :: follower :: otherUsers <- userKeys
      .zip(tokens)
      .traverse { case (key, token) =>
        for
          password <- passwordGen
          user <- userGen(key.email, Some(token), key.username)
          userWithPassword <- userWithHashPasswordGen(user, password)
        yield UserData(password, key.userId, userWithPassword)
      }
    allUsers = followed :: follower :: otherUsers
    following <- Gen.oneOf(true, false)
    followers = Map(
      followed.userId -> ((if following then List(follower.userId)
                           else List.empty) ++ otherUsers.map(_.userId)),
    )
    authorization = Authorization(
      Token(
        AuthScheme.Bearer,
        follower.userWithPassword.user.token.map(_.value.value).getOrElse(""),
      ),
    )
    initialState = HttpAppState.empty
      .setFollowers(followers)
      .setPasswords(allUsers.map { case UserData(password, _, userWithPassword) =>
        password -> userWithPassword.password
      }.toMap)
      .setTokens(
        allUsers
          .map { case UserData(_, _, userWithPassword) =>
            userWithPassword.user.email -> userWithPassword.user.token
          }
          .collect { case (email, Some(token)) => email -> token }
          .toMap,
      )
      .setUsersWithPassword(
        List.empty,
        allUsers.map { case UserData(_, userId, userWithPassword) =>
          userId -> userWithPassword
        }.toMap,
      )
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
