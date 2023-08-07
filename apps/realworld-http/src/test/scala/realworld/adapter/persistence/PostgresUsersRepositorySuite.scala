package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepositorySuite.{
  createUserTestCaseGen,
  findUserWithPasswordTestCaseGen,
  updateUserTestCaseGen,
}
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.Password.CipherText
import realworld.domain.model.RealWorldGenerators.{
  emailGen,
  userGen,
  usernameGen,
  userWithHashPasswordGen,
}
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, User}
import realworld.domain.service.UsersRepository.AlreadyInUseError
import realworld.shared.spec.CollectionGenerators.nDistinct
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import io.github.arainko.ducktape.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

final class PostgresUsersRepositorySuite extends PostgresSuite:
  test("should create a new user"):
    forAllF(createUserTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val usersRepository = PostgresUsersRepository(transactor)
        usersRepository.create(testCase.newUser).assertEquals(testCase.expected)

  test("should fail with an error when a duplicated email is created"):
    forAllF(
      for
        testCase <- createUserTestCaseGen
        otherUsername <- usernameGen.retryUntil(_ != testCase.newUser.user.username, 100)
      yield testCase.copy(newUser =
        testCase.newUser.copy(user = testCase.newUser.user.copy(username = otherUsername)),
      ),
    ): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testRepository.add(testCase.row)
          obtained <- usersRepository.create(testCase.newUser)
        yield obtained)
          .interceptMessage[AlreadyInUseError](
            "Given data is already in use: users_email_must_be_different",
          )
          .map(_ => ())

  test("should fail with an error when a duplicated username is created"):
    forAllF(
      for
        testCase <- createUserTestCaseGen
        otherEmail <- emailGen.retryUntil(_ != testCase.newUser.user.email, 100)
      yield testCase.copy(newUser =
        testCase.newUser.copy(user = testCase.newUser.user.copy(email = otherEmail)),
      ),
    ): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testRepository.add(testCase.row)
          obtained <- usersRepository.create(testCase.newUser)
        yield obtained)
          .interceptMessage[AlreadyInUseError](
            "Given data is already in use: users_username_must_be_different",
          )
          .map(_ => ())

  test("should find a user by email"):
    forAllF(findUserWithPasswordTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserBy(testCase.email)
        yield obtained).assertEquals(testCase.expected.map(_.user))

  test("should find a user with her password by email"):
    forAllF(findUserWithPasswordTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserWithPasswordBy(testCase.email)
        yield obtained).assertEquals(testCase.expected)

  test("should update an existent user"):
    forAllF(updateUserTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.update(testCase.updated)
          recovered <- usersRepository.findUserWithPasswordBy(testCase.updated.user.email)
        yield (obtained, recovered)).assertEquals((testCase.expected.user, Some(testCase.expected)))

object PostgresUsersRepositorySuite:
  private val userIdGen = Gen.choose(1, 10000)

  final private case class CreateUserTestCase(
      expected: User,
      newUser: UserWithHashPassword,
      row: UserRow,
  )

  private val createUserTestCaseGen = for
    userId <- userIdGen
    userWithPassword <- userWithHashPasswordGen(userGen =
      userGen(tokenGen = None, bioGen = None, imageGen = None),
    )
    expected = userWithPassword.user
    row = userWithPassword.toUserRow(userId)
  yield CreateUserTestCase(expected, userWithPassword, row)

  final private case class FindUserWithPasswordTestCase(
      email: Email,
      expected: Option[UserWithHashPassword],
      rows: List[UserRow],
  )

  private val findUserWithPasswordTestCaseGen = for
    emails <- nDistinct(7, emailGen)
    userIds <- nDistinct(7, userIdGen)
    selectedEmail :: otherEmails = emails: @unchecked
    selectedUserWithPassword <- userWithHashPasswordGen(userGen =
      userGen(emailGen = selectedEmail, tokenGen = None),
    )
    otherUsersWithPassword <- otherEmails.traverse(email =>
      userWithHashPasswordGen(userGen = userGen(emailGen = email, tokenGen = None)),
    )
    expected = Some(selectedUserWithPassword)
    rows = (selectedUserWithPassword :: otherUsersWithPassword)
      .zip(userIds)
      .map:
        case (userWithPassword, userId) => userWithPassword.toUserRow(userId)
  yield FindUserWithPasswordTestCase(selectedEmail, expected, rows)

  final private case class UpdateUserTestCase(
      expected: UserWithHashPassword,
      rows: List[UserRow],
      updated: UserWithHashPassword,
  )

  private val updateUserTestCaseGen = for
    emails <- nDistinct(7, emailGen)
    userIds <- nDistinct(7, userIdGen)
    selectedEmail :: otherEmails = emails: @unchecked
    selectedUserWithPassword <- userWithHashPasswordGen(userGen =
      userGen(emailGen = selectedEmail, tokenGen = None),
    )
    otherUsersWithPassword <- otherEmails.traverse(email =>
      userWithHashPasswordGen(userGen = userGen(emailGen = email, tokenGen = None)),
    )
    updated <- userWithHashPasswordGen(userGen(emailGen = selectedEmail, tokenGen = None))
      .retryUntil(_ != selectedUserWithPassword, 100)
    expected = updated
    rows = (selectedUserWithPassword :: otherUsersWithPassword)
      .zip(userIds)
      .map:
        case (userWithPassword, userId) => userWithPassword.toUserRow(userId)
  yield UpdateUserTestCase(expected, rows, updated)

  extension (userWithPassword: UserWithHashPassword)
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
