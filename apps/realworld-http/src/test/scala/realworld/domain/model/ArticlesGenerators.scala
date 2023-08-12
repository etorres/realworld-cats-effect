package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Article.*
import realworld.domain.model.Moment.{Created, Updated}
import realworld.domain.model.UsersGenerators.userIdGen
import realworld.shared.spec.CollectionGenerators.nDistinct
import realworld.shared.spec.StringGenerators.{alphaLowerStringBetween, alphaNumericStringBetween}
import realworld.shared.spec.TemporalGenerators.{localDateTimeAfter, localDateTimeGen}

import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances

object ArticlesGenerators:
  private val articleIdGen: Gen[ArticleId] = Gen.choose(1, 10000).map(ArticleId.unsafeFrom)

  private val slugGen = categoryGen().map(Slug.unsafeFrom)

  private val titleGen = textGen(3).map(Title.unsafeFrom)

  private val bodyGen = textGen(7).map(Body.unsafeFrom)

  private val tagGen = categoryGen().map(Tag.unsafeFrom)

  private def textGen(maxWords: Int = 10) = for
    numWords <- Gen.choose(1, maxWords)
    text <- Gen.listOfN(numWords, alphaNumericStringBetween(3, 12)).map(_.mkString(" "))
  yield text

  private def categoryGen(maxWords: Int = 3) = for
    numWords <- Gen.choose(1, maxWords)
    category <- Gen
      .listOfN(numWords, alphaLowerStringBetween(3, 12))
      .map(_.mkString("-"))
  yield category

  final case class ArticleKey(articleId: ArticleId, slug: Slug, title: Title)

  def uniqueArticleKeys(size: Int): Gen[List[ArticleKey]] = for
    articleIds <- nDistinct(size, articleIdGen)
    slugs <- nDistinct(size, slugGen)
    titles <- nDistinct(size, titleGen)
    articleKeys = articleIds
      .lazyZip(slugs)
      .lazyZip(titles)
      .toList
      .map { case (x, y, z) => ArticleKey(x, y, z) }
  yield articleKeys

  final case class ArticleContent(
      articleId: ArticleId,
      slug: Slug,
      title: Title,
      description: Option[String],
      body: Body,
      createdAt: Moment[Created],
      updatedAt: Moment[Updated],
      authorId: UserId,
  )

  final case class ArticleData(
      content: ArticleContent,
      favorites: List[UserId],
      tags: List[Tag],
  )

  private def articleDataGen(
      articleIdGen: Gen[ArticleId] = articleIdGen,
      authorIdGen: Gen[UserId] = userIdGen,
      favoritesGen: Gen[List[UserId]] = Gen.frequency(
        1 -> (for
          numFavorites <- Gen.choose(1, 3)
          favorites <- nDistinct(numFavorites, userIdGen)
        yield favorites),
        1 -> List.empty,
      ),
  ) = for
    articleId <- articleIdGen
    slug <- slugGen
    title <- titleGen
    description <- Gen.frequency(1 -> Gen.some(alphaNumericStringBetween(3, 12)), 1 -> None)
    body <- bodyGen
    createdAt <- localDateTimeGen.map(Moment.unsafeFrom[Created])
    updatedAt <- localDateTimeAfter(createdAt.value).map(Moment.unsafeFrom[Updated])
    authorId <- authorIdGen
    favorites <- favoritesGen
    tags <- Gen.frequency(
      1 -> (for
        numTags <- Gen.choose(1, 3)
        tags <- nDistinct(numTags, tagGen)
      yield tags),
      1 -> List.empty,
    )
  yield ArticleData(
    ArticleContent(articleId, slug, title, description, body, createdAt, updatedAt, authorId),
    favorites,
    tags,
  )

  def uniqueArticleData(size: Int, userIdGen: Gen[UserId]): Gen[List[ArticleData]] = for
    articleKeys <- uniqueArticleKeys(size)
    articleData <- articleKeys.traverse(key =>
      articleDataGen(
        articleIdGen = key.articleId,
        authorIdGen = userIdGen,
        favoritesGen = Gen.frequency(
          1 -> (for
            numFavorites <- Gen.choose(1, 3)
            favorites <- nDistinct(numFavorites, userIdGen)
          yield favorites),
          1 -> List.empty,
        ),
      ),
    )
  yield articleData
