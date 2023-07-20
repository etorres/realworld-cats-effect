package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{AccessDecision, UserCredentials}

import cats.effect.IO

final class UsersService:
  def grantAccessIdentifiedBy(userCredentials: UserCredentials): IO[AccessDecision] =
    IO.pure(AccessDecision(userCredentials.email, AccessDecision.Access.Forbidden)) // TODO
