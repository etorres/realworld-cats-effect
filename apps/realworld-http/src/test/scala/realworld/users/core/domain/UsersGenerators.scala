package es.eriktorr
package realworld.users.core.domain

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.spec.CollectionGenerators.nDistinct
import realworld.spec.StringGenerators.{alphaNumericStringBetween, alphaNumericStringShorterThan}
import realworld.spec.UriGenerator.uriGen
import realworld.users.core.domain.Password.PlainText
import realworld.users.core.domain.User.{Token, Username}
import realworld.users.core.domain.UserWithPassword.UserWithHashPassword

import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances

import java.net.URI

object UsersGenerators:
  val emailGen: Gen[Email] = for
    domain <- Gen.oneOf("example.com", "example.net", "example.org")
    username <- alphaNumericStringBetween(3, 12)
  yield Email.unsafeFrom(s"$username@$domain")

  val passwordGen: Gen[Password[PlainText]] =
    alphaNumericStringBetween(3, 12).map(Password.unsafeFrom[PlainText])

  val tokenGen: Gen[Token] = alphaNumericStringBetween(3, 12).map(Token.unsafeFrom)

  val userIdGen: Gen[UserId] = Gen.choose(1, 10000).map(UserId.unsafeFrom)

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

  def userWithHashPasswordGen(
      userGen: Gen[User] = userGen(),
      passwordGen: Gen[Password[PlainText]] = passwordGen,
  ): Gen[UserWithHashPassword] = for
    user <- userGen
    password <- passwordGen
    hash = Password.cipher(password).orFail
  yield UserWithHashPassword(user, hash)

  final case class UserKey(email: Email, userId: UserId, username: Username)

  def uniqueUserKeys(size: Int): Gen[List[UserKey]] = for
    emails <- nDistinct(size, emailGen)
    userIds <- nDistinct(size, userIdGen)
    usernames <- nDistinct(size, usernameGen)
    userKeys = emails
      .lazyZip(userIds)
      .lazyZip(usernames)
      .toList
      .map { case (x, y, z) => UserKey(x, y, z) }
  yield userKeys

  final case class UserData(
      password: Password[PlainText],
      userId: UserId,
      userWithPassword: UserWithHashPassword,
  )

  def uniqueUserData(size: Int): Gen[List[UserData]] = for
    userKeys <- uniqueUserKeys(size)
    tokens <- nDistinct(size, tokenGen)
    userData <- userKeys
      .zip(tokens)
      .traverse { case (key, token) =>
        for
          password <- passwordGen
          user <- userGen(key.email, Some(token), key.username)
          userWithPassword <- userWithHashPasswordGen(user, password)
        yield UserData(password, key.userId, userWithPassword)
      }
  yield userData

  final case class UserWithId(userId: UserId, userWithPassword: UserWithHashPassword)

  def uniqueTokenLessUsersWithId(size: Int): Gen[List[UserWithId]] = for
    userKeys <- uniqueUserKeys(size)
    usersWithId <- userKeys.traverse: key =>
      for
        user <- userGen(key.email, None, key.username)
        userWithPassword <- userWithHashPasswordGen(user)
      yield UserWithId(key.userId, userWithPassword)
  yield usersWithId
