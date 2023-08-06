package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.{CipherText, PlainText}
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

  val passwordGen: Gen[Password[PlainText]] =
    alphaNumericStringBetween(3, 12).map(Password.unsafeFrom[PlainText])

  val tokenGen: Gen[Token] = alphaNumericStringBetween(3, 12).map(Token.unsafeFrom)

  val usernameGen: Gen[Username] = alphaNumericStringBetween(3, 12).map(Username.unsafeFrom)

  def userGen(
      emailGen: Gen[Email] = emailGen,
      tokenGen: Gen[Option[Token]] =
        Gen.frequency(1 -> Gen.some(tokenGen), 1 -> Option.empty[Token]),
      usernameGen: Gen[Username] = usernameGen,
      bioGen: Gen[Option[String]] =
        Gen.frequency(1 -> Gen.some(alphaNumericStringShorterThan(24)), 1 -> Option.empty[String]),
      imageGen: Gen[Option[URI]] = Gen.frequency(1 -> Gen.some(uriGen()), 1 -> Option.empty[URI]),
  ): Gen[User] =
    for
      email <- emailGen
      token <- tokenGen
      username <- usernameGen
      bio <- bioGen
      image <- imageGen
    yield User(email, token, username, bio, image)

  def userWithPasswordGen(
                           userGen: Gen[User] = userGen(),
                           passwordGen: Gen[Password[PlainText]] = passwordGen,
  ): Gen[UserWithPassword[CipherText]] = for
    user <- userGen
    password <- passwordGen
    hash = Password.cipher(password).orFail
  yield UserWithPassword(user, hash)
