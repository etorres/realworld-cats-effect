package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.ClearText
import realworld.domain.model.User.{Token, Username}
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.shared.spec.StringGenerators.{
  alphaNumericStringBetween,
  alphaNumericStringShorterThan,
}
import realworld.shared.spec.UriGenerator.uriGen

import org.scalacheck.Gen

import java.net.URI

object RealWorldGenerators:
  val emailGen: Gen[Email] = for
    domain <- Gen.oneOf("example.com", "example.net", "example.org")
    username <- alphaNumericStringBetween(3, 12)
  yield Email.unsafeFrom(s"$username@$domain")

  val passwordGen: Gen[Password[ClearText]] =
    alphaNumericStringBetween(3, 12).map(Password.unsafeFrom[ClearText])

  val tokenGen: Gen[Token] = alphaNumericStringBetween(3, 12).map(Token.unsafeFrom)

  def userGen(
      emailGen: Gen[Email] = emailGen,
      tokenGen: Gen[Option[Token]] =
        Gen.frequency(1 -> Gen.some(tokenGen), 1 -> Option.empty[Token]),
  ): Gen[User] =
    for
      email <- emailGen
      token <- tokenGen
      username <- alphaNumericStringBetween(3, 12).map(Username.unsafeFrom)
      bio <- alphaNumericStringShorterThan(24)
      image <- Gen.frequency(1 -> Gen.some(uriGen()), 1 -> Option.empty[URI])
    yield User(email, token, username, bio, image)

  def userWithPasswordGen(
      userGen: Gen[User] = userGen(),
      passwordGen: Gen[Password[ClearText]] = passwordGen,
  ): Gen[UserWithPassword] = for
    user <- userGen
    password <- passwordGen
    hash = Password.cipher(password).orFail
  yield UserWithPassword(user, hash)
