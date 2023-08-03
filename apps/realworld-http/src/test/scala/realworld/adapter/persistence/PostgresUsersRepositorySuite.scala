package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepositorySuite.testCaseGen
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.RealWorldGenerators.{emailGen, userGen, userWithPasswordGen}
import realworld.domain.model.{Email, UserWithPassword}
import realworld.shared.spec.CollectionGenerators.nDistinct
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

final class PostgresUsersRepositorySuite extends PostgresSuite:
  test("should find a user with her password by email"):
    forAllF(testCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val testRepository = PostgresUsersTestRepository(transactor)
        val repository = PostgresUsersRepository(transactor)
        (for
          _ <- testCase.rows.traverse_(testRepository.add)
          obtained <- repository.findUserWithPasswordBy(testCase.email)
        yield obtained).assertEquals(testCase.expected)

object PostgresUsersRepositorySuite:
  final private case class TestCase(
      email: Email,
      expected: Option[UserWithPassword],
      rows: List[UserRow],
  )

  private val testCaseGen = for
    emails <- nDistinct(7, emailGen)
    userIds <- nDistinct(7, Gen.choose(1, 10000))
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
        case (userWithPassword, userId) =>
          UserRow(
            userId = userId,
            email = userWithPassword.user.email,
            username = userWithPassword.user.username,
            password = userWithPassword.password.value.value,
            bio = userWithPassword.user.bio,
            image = userWithPassword.user.image.map(_.toString),
          )
  yield TestCase(selectedEmail, expected, rows)
