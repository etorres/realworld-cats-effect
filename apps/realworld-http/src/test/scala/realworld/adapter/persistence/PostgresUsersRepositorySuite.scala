package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepositorySuite.{
  createUserTestCaseGen,
  findUserIdTestCaseGen,
  findUserWithPasswordTestCaseGen,
  updateUserTestCaseGen,
}
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.RealWorldGenerators.{
  emailGen,
  userGen,
  usernameGen,
  userWithHashPasswordGen,
}
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, User, UserId}
import realworld.domain.service.UsersRepository.AlreadyInUseError
import realworld.shared.spec.CollectionGenerators.nDistinct
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import monocle.syntax.all.{focus, refocus}
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

  test("should find a user Id by email"):
    forAllF(findUserIdTestCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val usersRepository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- usersRepository.findUserIdBy(testCase.email)
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
          obtained <- usersRepository.update(testCase.updated, ???)
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

  final private case class FindUserIdTestCase(
      email: Email,
      expected: Option[UserId],
      rows: List[UserRow],
  )

  private val findUserIdTestCaseGen = for
    emails <- nDistinct(7, emailGen)
    selectedEmail :: otherEmails = emails: @unchecked
    userIds <- nDistinct(7, userIdGen)
    selectedUserId :: otherUserIds = userIds: @unchecked
    selectedUserWithPassword <- userWithHashPasswordGen(userGen =
      userGen(emailGen = selectedEmail, tokenGen = None),
    )
    otherUsersWithPassword <- otherEmails.traverse(email =>
      userWithHashPasswordGen(userGen = userGen(emailGen = email, tokenGen = None)),
    )
    expected = Some(UserId.unsafeFrom(selectedUserId))
    rows = ((selectedUserWithPassword -> selectedUserId) :: otherUsersWithPassword
      .zip(otherUserIds))
      .map:
        case (userWithPassword, userId) => userWithPassword.toUserRow(userId)
  yield FindUserIdTestCase(selectedEmail, expected, rows)

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
      val user = userWithPassword.user
      UserRow(
        userId,
        user.email,
        user.username,
        userWithPassword.password.value,
        user.bio,
        user.image.map(_.toString),
      )
