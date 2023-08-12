package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.Article

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ListArticlesResponse(articles: List[Article], articlesCount: Int)

object ListArticlesResponse:
  given getArticlesResponseJsonDecoder: Decoder[ListArticlesResponse] = deriveDecoder

  given getArticlesResponseJsonEncoder: Encoder[ListArticlesResponse] = deriveEncoder
