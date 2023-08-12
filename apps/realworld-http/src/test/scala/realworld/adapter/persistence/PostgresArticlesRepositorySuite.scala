package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresArticlesRepositorySuite.testCaseGen
import realworld.adapter.persistence.row.*
import realworld.domain.model.Article.Author
import realworld.domain.model.ArticlesGenerators.{uniqueArticleData, ArticleData}
import realworld.domain.model.UsersGenerators.{uniqueTokenLessUsersWithId, UserWithId}
import realworld.domain.model.{Article, UserId}
import realworld.domain.service.{ArticlesFilters, Pagination}
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

final class PostgresArticlesRepositorySuite extends PostgresSuite:
  test("Should find articles using the given filters"):
    forAllF(testCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val usersTestRepository = PostgresUsersTestRepository(transactor)
        val followersTestRepository = PostgresFollowersTestRepository(transactor)
        val articlesTestRepository = PostgresArticlesTestRepository(transactor)
        val articlesRepository = PostgresArticlesRepository(transactor)
        (for
          _ <- testCase.userRows.traverse_(usersTestRepository.add)
          _ <- testCase.followerRows.traverse_(followersTestRepository.add)
          _ <- testCase.articleRows.traverse_(articlesTestRepository.add)
          _ <- testCase.favoriteRows.traverse_(articlesTestRepository.add)
          _ <- testCase.tagRows.traverse_(articlesTestRepository.add)
          obtained <- articlesRepository.findArticlesBy(
            testCase.filters,
            testCase.pagination,
            testCase.userId,
          )
        yield obtained.sortBy(_.slug)).assertEquals(testCase.expected.sortBy(_.slug))

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
object PostgresArticlesRepositorySuite:
  final private case class TestCase(
      articleRows: List[ArticleRow],
      expected: List[Article],
      favoriteRows: List[FavoriteRow],
      filters: ArticlesFilters,
      followerRows: List[FollowerRow],
      pagination: Pagination,
      tagRows: List[TagRow],
      userId: UserId,
      userRows: List[UserRow],
  )

  private val testCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    allArticles <- uniqueArticleData(7, Gen.oneOf(allUsers.map(_.userId)))
    allFollowers <- allUsers
      .map(_.userId)
      .flatTraverse(bob =>
        allUsers
          .map(_.userId)
          .traverse(alice =>
            if bob != alice then Gen.frequency(1 -> Some(bob -> alice), 1 -> None)
            else Gen.const(None),
          ),
      )
      .map(_.collect { case Some(value) => value })
      .map(_.groupBy(_._1))
      .map(_.view.mapValues(_.map(_._2)).toMap)
    filters = ArticlesFilters(None, None, None)
    pagination = Pagination.default
    articleRows = allArticles.map:
      case ArticleData(content, _, _) =>
        ArticleRow(
          content.articleId,
          content.slug,
          content.title,
          content.description,
          content.body,
          content.createdAt.value,
          content.updatedAt.value,
          content.authorId,
        )
    favoriteRows = allArticles.flatMap:
      case ArticleData(content, favorites, _) =>
        favorites.map(profileId => FavoriteRow(profileId, content.articleId))
    followerRows = allFollowers.toList.flatMap:
      case (followed, followers) => followers.map(FollowerRow(followed, _))
    tagRows = allArticles.flatMap:
      case ArticleData(content, _, tags) =>
        tags.map(tag => TagRow(tag, content.articleId))
    userRows = allUsers.map:
      case UserWithId(userId, userWithPassword) =>
        val user = userWithPassword.user
        UserRow(
          userId,
          user.email,
          user.username,
          userWithPassword.password.value,
          user.bio,
          user.image.map(_.toString),
        )
    expected = allArticles.map:
      case ArticleData(content, favorites, tags) =>
        val author = allUsers
          .find(_.userId == content.authorId)
          .map(_.userWithPassword.user)
          .getOrElse(throw IllegalStateException(s"Author Id not found: ${content.authorId}"))
        Article(
          content.slug,
          content.title,
          content.description,
          content.body,
          tags,
          content.createdAt,
          content.updatedAt,
          favorites.contains(selectedUser.userId),
          favorites.length,
          Author(
            author.username,
            author.bio,
            author.image,
            allFollowers.get(content.authorId).exists(_.contains(selectedUser.userId)),
          ),
        )
  yield TestCase(
    articleRows,
    expected,
    favoriteRows,
    filters,
    followerRows,
    pagination,
    tagRows,
    selectedUser.userId,
    userRows,
  )
