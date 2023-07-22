package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{Email, UserWithPassword}

import cats.effect.IO

trait UsersRepository:
  def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword]]
