package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepositorySuite.*
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.UsersGenerators.*
import realworld.domain.model.{Email, User, UserId}
import realworld.domain.service.UsersRepository.AlreadyInUseError
import realworld.shared.spec.PostgresSuite

import cats.implicits.toFoldableOps
import monocle.syntax.all.{focus, refocus}
import org.scalacheck.Gen
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
        username = testCase.focus(_.newUser.user.username)
        otherUsernameTestCase <- usernameGen
          .retryUntil(_ != username.get, 100)
          .map: otherUsername =>
            testCase.copy(newUser = username.replace(otherUsername).newUser)
      yield otherUsernameTestCase,
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
        email = testCase.focus(_.newUser.user.email)
        otherEmailTestCase <- emailGen
          .retryUntil(_ != email.get, 100)
          .map: otherEmail =>
            testCase.copy(newUser = email.replace(otherEmail).newUser)
      yield otherEmailTestCase,
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

  test("should find a user by her Id"):
    forAllF(findUserTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserBy(testCase.query)
        yield obtained).assertEquals(testCase.expected)

  test("should find a user with her Id by username"):
    forAllF(findUserByUsernameTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserWithIdBy(testCase.query)
        yield obtained).assertEquals(testCase.expected)

  test("should find a user Id by email"):
    forAllF(findUserIdTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserIdBy(testCase.query)
        yield obtained).assertEquals(testCase.expected)

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
          obtained <- usersRepository.update(testCase.updated, testCase.userId)
          recovered <- usersRepository.findUserWithPasswordBy(testCase.updated.user.email)
        yield (obtained, recovered)).assertEquals((testCase.expected.user, Some(testCase.expected)))

object PostgresUsersRepositorySuite:
  final private case class CreateUserTestCase(
      expected: User,
      newUser: UserWithHashPassword,
      row: UserRow,
  )

  final private case class FindTestCase[A, B](
      expected: Option[B],
      rows: List[UserRow],
      query: A,
  )

  private val createUserTestCaseGen = for
    userId <- userIdGen
    userWithPassword <- userWithHashPasswordGen(userGen =
      userGen(tokenGen = None, bioGen = None, imageGen = None),
    )
    expected = userWithPassword.user
    row = userWithPassword.toUserRow(userId)
  yield CreateUserTestCase(expected, userWithPassword, row)

  private val findUserTestCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    expected = Some(selectedUser.userWithPassword.user)
    rows = allUsers.map:
      case UserWithId(userId, userWithPassword) => userWithPassword.toUserRow(userId)
  yield FindTestCase(expected, rows, selectedUser.userId)

  private val findUserByUsernameTestCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    expected = Some(selectedUser.userWithPassword.user -> selectedUser.userId)
    rows = allUsers.map:
      case UserWithId(userId, userWithPassword) => userWithPassword.toUserRow(userId)
  yield FindTestCase(expected, rows, selectedUser.userWithPassword.user.username)

  private val findUserIdTestCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    expected = Some(selectedUser.userId)
    rows = allUsers.map:
      case UserWithId(userId, userWithPassword) => userWithPassword.toUserRow(userId)
  yield FindTestCase(expected, rows, selectedUser.userWithPassword.user.email)

  final private case class FindUserWithPasswordTestCase(
      email: Email,
      expected: Option[UserWithHashPassword],
      rows: List[UserRow],
  )

  private val findUserWithPasswordTestCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    expected = Some(selectedUser.userWithPassword)
    rows = allUsers.map:
      case UserWithId(userId, userWithPassword) => userWithPassword.toUserRow(userId)
  yield FindUserWithPasswordTestCase(selectedUser.userWithPassword.user.email, expected, rows)

  final private case class UpdateUserTestCase(
      expected: UserWithHashPassword,
      rows: List[UserRow],
      updated: UserWithHashPassword,
      userId: UserId,
  )

  private val updateUserTestCaseGen = for
    case selectedUser :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = selectedUser :: otherUsers
    updated <- userWithHashPasswordGen(
      userGen(emailGen = selectedUser.userWithPassword.user.email, tokenGen = None),
    ).retryUntil(_ != selectedUser.userWithPassword, 100)
    expected = updated
    rows = allUsers.map:
      case UserWithId(userId, userWithPassword) => userWithPassword.toUserRow(userId)
  yield UpdateUserTestCase(expected, rows, updated, selectedUser.userId)

  extension (userWithPassword: UserWithHashPassword)
    def toUserRow(userId: Int): UserRow =
      val user = userWithPassword.user
      UserRow(
        userId,
        user.email,
        user.username,
        userWithPassword.password.value,
        user.bio,
        user.image.map(_.toString),
      )
