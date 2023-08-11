package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.Article

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class GetArticlesResponse(articles: List[Article], articlesCount: Int)

object GetArticlesResponse:
  given getArticlesResponseJsonDecoder: Decoder[GetArticlesResponse] = deriveDecoder

  given getArticlesResponseJsonEncoder: Encoder[GetArticlesResponse] = deriveEncoder
