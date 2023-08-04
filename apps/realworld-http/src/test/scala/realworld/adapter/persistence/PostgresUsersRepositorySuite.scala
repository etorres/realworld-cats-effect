package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepositorySuite.{
  loginTestCaseGen,
  registerTestCaseGen,
}
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.RealWorldGenerators.{
  emailGen,
  userGen,
  usernameGen,
  userWithPasswordGen,
}
import realworld.domain.model.{Email, NewUser, User, UserWithPassword}
import realworld.domain.service.UsersRepository.AlreadyInUseError
import realworld.shared.spec.CollectionGenerators.nDistinct
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import io.github.arainko.ducktape.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

final class PostgresUsersRepositorySuite extends PostgresSuite:
  test("should find a user with her password by email"):
    forAllF(loginTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserWithPasswordBy(testCase.email)
        yield obtained).assertEquals(testCase.expected)

  test("should register a new user"):
    forAllF(registerTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val usersRepository = PostgresUsersRepository(transactor)
        usersRepository.register(testCase.newUser).assertEquals(testCase.expected)

  test("should fail with an error when a duplicated email is registered"):
    forAllF(
      for
        testCase <- registerTestCaseGen
        otherUsername <- usernameGen.retryUntil(_ != testCase.newUser.username, 100)
      yield testCase.copy(newUser = testCase.newUser.copy(username = otherUsername)),
    ): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testRepository.add(testCase.row)
          obtained <- usersRepository.register(testCase.newUser)
        yield obtained)
          .interceptMessage[AlreadyInUseError](
            "Given data is already in use: users_email_must_be_different",
          )
          .map(_ => ())

  test("should fail with an error when a duplicated username is registered"):
    forAllF(
      for
        testCase <- registerTestCaseGen
        otherEmail <- emailGen.retryUntil(_ != testCase.newUser.email, 100)
      yield testCase.copy(newUser = testCase.newUser.copy(email = otherEmail)),
    ): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testRepository.add(testCase.row)
          obtained <- usersRepository.register(testCase.newUser)
        yield obtained)
          .interceptMessage[AlreadyInUseError](
            "Given data is already in use: users_username_must_be_different",
          )
          .map(_ => ())

object PostgresUsersRepositorySuite:
  private val userIdGen = Gen.choose(1, 10000)

  final private case class LoginTestCase(
      email: Email,
      expected: Option[UserWithPassword],
      rows: List[UserRow],
  )

  private val loginTestCaseGen = for
    emails <- nDistinct(7, emailGen)
    userIds <- nDistinct(7, userIdGen)
    selectedEmail :: otherEmails = emails: @unchecked
    selectedUserWithPassword <- userWithPasswordGen(userGen =
      userGen(emailGen = selectedEmail, tokenGen = None),
    )
    otherUsersWithPassword <- otherEmails.traverse(email =>
      userWithPasswordGen(userGen = userGen(emailGen = email, tokenGen = None)),
    )
    expected = Some(selectedUserWithPassword)
    rows = (selectedUserWithPassword :: otherUsersWithPassword)
      .zip(userIds)
      .map:
        case (userWithPassword, userId) => userWithPassword.toUserRow(userId)
  yield LoginTestCase(selectedEmail, expected, rows)

  final private case class RegisterTestCase(expected: User, newUser: NewUser, row: UserRow)

  private val registerTestCaseGen = for
    userId <- userIdGen
    userWithPassword <- userWithPasswordGen(userGen =
      userGen(tokenGen = None, bioGen = None, imageGen = None),
    )
    expected = userWithPassword.user
    newUser = userWithPassword.toNewUser
    row = userWithPassword.toUserRow(userId)
  yield RegisterTestCase(expected, newUser, row)

  extension (userWithPassword: UserWithPassword)
    def toNewUser: NewUser =
      userWithPassword
        .into[NewUser]
        .transform(
          Field.computed(_.email, _.user.email),
          Field.computed(_.username, _.user.username),
        )

    def toUserRow(userId: Int): UserRow =
      userWithPassword
        .into[UserRow]
        .transform(
          Field.const(_.userId, userId),
          Field.computed(_.email, _.user.email),
          Field.computed(_.username, _.user.username),
          Field.computed(_.password, _.password.value),
          Field.computed(_.bio, _.user.bio),
          Field.computed(_.image, _.user.image.map(_.toString)),
        )
