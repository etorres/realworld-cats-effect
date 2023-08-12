package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresArticlesRepositorySuite.*
import realworld.adapter.persistence.row.*
import realworld.domain.model.Article.{Author, Tag}
import realworld.domain.model.ArticlesGenerators.{uniqueArticleData, ArticleContent, ArticleData}
import realworld.domain.model.FollowersGenerators.followersGen
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.UsersGenerators.{uniqueTokenLessUsersWithId, UserWithId}
import realworld.domain.model.{Article, UserId, UserWithPassword}
import realworld.domain.service.Pagination.{Limit, Offset}
import realworld.domain.service.{ArticlesFilters, Pagination}
import realworld.shared.spec.PostgresSuite

import cats.implicits.toFoldableOps
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

import scala.util.chaining.scalaUtilChainingOps

final class PostgresArticlesRepositorySuite extends PostgresSuite:
  test("Should find all articles"):
    testWith(findAllArticlesTestCaseGen)

  test("Should find articles filtered by favorited"):
    testWith(filterByFavoritedTestCaseGen)

  test("Should find articles filtered by author"):
    testWith(filterByAuthorTestCaseGen)

  test("Should find articles filtered by tag"):
    testWith(filterByTagTestCaseGen)

  test("Should find articles within the specified range"):
    testWith(paginationTestCaseGen)

  private def testWith(testCaseGen: Gen[TestCase]) =
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
          // TODO
          _ = println(s"\n >> FILTERS: ${testCase.filters}\n")
          _ = println(s"\n >> OBTAINED: ${obtained.length}\n")
          _ = println(s"\n >> EXPECTED: ${testCase.expected.length}\n")
        // TODO
        yield obtained.sortBy(_.slug)).assertEquals(testCase.expected.sortBy(_.slug))

  // TODO: test pagination

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

  private val findAllArticlesTestCaseGen =
    testCaseGen((_, _, _) => Gen.const(ArticlesFilters(None, None, None)))

  private val filterByAuthorTestCaseGen = testCaseGen { case (authors, _, _) =>
    filtersGen(Gen.some(Gen.oneOf(authors)), None, None)
  }

  private val filterByFavoritedTestCaseGen = testCaseGen { case (_, favorited, _) =>
    filtersGen(None, Gen.some(Gen.oneOf(favorited)), None)
  }

  private val filterByTagTestCaseGen = testCaseGen { case (_, _, tags) =>
    filtersGen(None, None, Gen.some(Gen.oneOf(tags)))
  }

  private val paginationTestCaseGen =
    testCaseGen(
      (_, _, _) => Gen.const(ArticlesFilters(None, None, None)),
      Gen.const(Pagination(Limit.unsafeFrom(2), Offset.unsafeFrom(1))),
    )

  private def testCaseGen(
      filtersGen: (List[Username], List[Username], List[Tag]) => Gen[ArticlesFilters],
      paginationGen: Gen[Pagination] = Gen.const(Pagination.default),
  ) = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    allArticles <- uniqueArticleData(7, Gen.oneOf(allUsers.map(_.userId)))
    allFollowers <- followersGen(allUsers.map(_.userId))
    filters <-
      val users = allUsers.map(x => x.userId -> x.userWithPassword.user.username).toMap
      val authors =
        allArticles.map(_.content.authorId).map(users.get).collect { case Some(value) => value }
      val favorited =
        allArticles.flatMap(_.favorites).map(users.get).collect { case Some(value) => value }
      val tags = allArticles.flatMap(_.tags)
      filtersGen(authors, favorited, tags)
    pagination <- paginationGen
    articleRows = allArticles.map:
      case ArticleData(content, _, _) => articleRowFrom(content)
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
        userRowFrom(userId, userWithPassword)
    expected = (filters.author.flatMap(username =>
      allUsers.find(_.userWithPassword.user.username == username).map(_.userId),
    ) match
      case Some(selectedAuthor) =>
        allArticles.filter:
          case ArticleData(content, _, _) => content.authorId == selectedAuthor
      case None => allArticles
    )
    .pipe: filtered =>
      filters.favorited.flatMap(username =>
        allUsers.find(_.userWithPassword.user.username == username).map(_.userId),
      ) match
        case Some(selectedFavorited) =>
          filtered.filter:
            case ArticleData(_, favorites, _) => favorites.contains(selectedFavorited)
        case None => filtered
    .pipe: filtered =>
      filters.tag match
        case Some(selectedTag) =>
          filtered.filter:
            case ArticleData(_, _, tags) => tags.contains(selectedTag)
        case None => filtered
    .map:
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
          tags.sorted,
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

  private def filtersGen(
      authorGen: Gen[Option[Username]],
      favoritedGen: Gen[Option[Username]],
      tagGen: Gen[Option[Tag]],
  ) = for
    author <- authorGen
    favorited <- favoritedGen
    tag <- tagGen
  yield ArticlesFilters(author, favorited, tag)

  private def articleRowFrom(content: ArticleContent) =
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

  private def userRowFrom(userId: UserId, userWithPassword: UserWithHashPassword) =
    val user = userWithPassword.user
    UserRow(
      userId,
      user.email,
      user.username,
      userWithPassword.password.value,
      user.bio,
      user.image.map(_.toString),
    )
