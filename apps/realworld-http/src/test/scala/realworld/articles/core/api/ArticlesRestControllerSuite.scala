package es.eriktorr
package realworld.articles.core.api

import realworld.articles.core.api.ArticlesRestControllerSuite.successfulListArticlesGen
import realworld.articles.core.api.response.ListArticlesResponse
import realworld.articles.core.domain.ArticlesFilters
import realworld.articles.core.domain.ArticlesGenerators.{articlesFrom, tagGen, uniqueArticleData}
import realworld.common.api.HttpAppSuite
import realworld.common.api.HttpAppSuite.{
  authorizationFor,
  tokensFrom,
  usersWithPasswordFrom,
  TestCase,
}
import realworld.common.api.HttpAppSuiteRunner.{runWith, HttpAppState}
import realworld.common.domain.Pagination
import realworld.common.domain.Pagination.{Limit, Offset}
import realworld.users.core.domain.UsersGenerators.{uniqueUserData, usernameGen, UserWithId}
import realworld.users.profiles.domain.FollowersGenerators.followersGen

import cats.effect.IO
import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{Method, Request, Status, Uri}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

import scala.util.Random

final class ArticlesRestControllerSuite extends HttpAppSuite:
  test("should list articles applying the given filters and navigation settings"):
    forAllF(successfulListArticlesGen): testCase =>
      given Decoder[ListArticlesResponse] = ListArticlesResponse.getArticlesResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(
            method = Method.GET,
            uri = Uri.unsafeFromString(s"/api/articles${testCase.request}"),
          ).putHeaders(testCase.authorization),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(result, Right(testCase.expectedResponse))
        assertEquals(finalState, testCase.expectedState)
      }

object ArticlesRestControllerSuite:
  private val successfulListArticlesGen = for
    case selectedUser :: otherUsers <- uniqueUserData(7)
    allUsers = selectedUser :: otherUsers
    allArticlesData <- uniqueArticleData(7, Gen.oneOf(allUsers.map(_.userId)))
    allFollowers <- followersGen(allUsers.map(_.userId))
    viewer <- Gen.frequency(1 -> Gen.some(selectedUser), 1 -> None)
    articles = articlesFrom(
      allArticlesData,
      allFollowers,
      allUsers.map(x => UserWithId(x.userId, x.userWithPassword)),
      viewer.map(_.userId),
    )
    filter <- filterGen
    pagination <- paginationGen
    authorization = viewer.map(x => authorizationFor(x.userWithPassword.user))
    initialState = HttpAppState.empty
      .setArticles(Map((filter, pagination.getOrElse(Pagination.default)) -> articles))
      .setTokens(tokensFrom(allUsers))
      .setUsersWithPassword(List.empty, usersWithPasswordFrom(allUsers))
    expectedState = initialState.copy()
    request = Random.shuffle(
      (
        filter.author.map(x => s"author=$x"),
        filter.favorited.map(x => s"favorited=$x"),
        filter.tag.map(x => s"tag=$x"),
        pagination.map { case Pagination(limit, _) => s"limit=$limit" },
        pagination.map { case Pagination(_, offset) => s"offset=$offset" },
      ).toList.collect { case Some(value) => value },
    ) match
      case ::(head, next) => (head :: next).mkString("?", "&", "")
      case Nil => ""
    expectedResponse = (ListArticlesResponse(articles, articles.length), Status.Ok)
  yield TestCase(authorization, initialState, expectedState, request, expectedResponse)

  private lazy val filterGen = for
    author <- Gen.frequency(1 -> Gen.some(usernameGen), 1 -> None)
    favorited <- Gen.frequency(1 -> Gen.some(usernameGen), 1 -> None)
    tag <- Gen.frequency(1 -> Gen.some(tagGen), 1 -> None)
  yield ArticlesFilters(author, favorited, tag)

  private lazy val paginationGen = Gen.frequency(
    1 -> Gen.some(for
      limit <- Gen.choose(1, 20).map(Limit.unsafeFrom)
      offset <- Gen.choose(0, 1000).map(Offset.unsafeFrom)
    yield Pagination(limit, offset)),
    1 -> None,
  )
