package es.eriktorr
package realworld.articles.core.api.response

import realworld.articles.core.domain.Article

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ListArticlesResponse(articles: List[Article], articlesCount: Int)

object ListArticlesResponse:
  given getArticlesResponseJsonDecoder: Decoder[ListArticlesResponse] = deriveDecoder

  given getArticlesResponseJsonEncoder: Encoder[ListArticlesResponse] = deriveEncoder
