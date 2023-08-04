package es.eriktorr
package realworld.application

import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.{Token, Username}
import realworld.domain.model.{Email, Password, User, UserWithPassword}

import com.softwaremill.diffx.Diff

import java.net.URI

object UsersDiffIgnoringPassword:
  given Diff[Email] = Diff.diffForString.contramap(identity)

  given Diff[Token] = Diff.diffForString.contramap(_.value.value)

  given Diff[Username] = Diff.diffForString.contramap(identity)

  given Diff[URI] = Diff.diffForString.contramap(_.toString)

  given Diff[User] = Diff.derived[User]

  given Diff[Password[CipherText]] = Diff.diffForString.contramap(_.value.value)

  given Diff[UserWithPassword] = Diff.derived[UserWithPassword].ignore(_.password)
