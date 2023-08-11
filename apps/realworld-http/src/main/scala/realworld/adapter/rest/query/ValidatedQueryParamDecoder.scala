package es.eriktorr
package realworld.adapter.rest.query

import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.data.NonEmptyList
import org.http4s.{ParseFailure, QueryParamDecoder}

object ValidatedQueryParamDecoder:
  def decodeQueryParamWith[A, B](
      fx: A => AllErrorsOr[B],
      param: String,
  )(using QueryParamDecoder[A]): QueryParamDecoder[B] =
    QueryParamDecoder[A].emapValidatedNel: value =>
      fx(value).leftMap: errors =>
        NonEmptyList.fromReducible(errors.map: cause =>
          ParseFailure(param, cause))
