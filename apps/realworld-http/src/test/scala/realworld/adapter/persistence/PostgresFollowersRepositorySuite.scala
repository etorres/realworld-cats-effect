package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresFollowersRepositorySuite.testCaseGen
import realworld.adapter.persistence.row.{FollowerRow, UserRow}
import realworld.domain.model.UserId
import realworld.domain.model.UsersGenerators.*
import realworld.shared.spec.PostgresSuite

import cats.implicits.toFoldableOps
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class PostgresFollowersRepositorySuite extends PostgresSuite:
  test("should check whether or not a user is following another user"):
    forAllF(testCaseGen): testCase =>
      testTransactor.resource.use: transactor =>
        val usersTestRepository = PostgresUsersTestRepository(transactor)
        val followersTestRepository = PostgresFollowersTestRepository(transactor)
        val followersRepository = PostgresFollowersRepository(transactor)
        (for
          _ <- testCase.userRows.traverse_(usersTestRepository.add)
          _ <- testCase.followerRows.traverse_(followersTestRepository.add)
          obtained <- followersRepository.isFollowing(testCase.followed, testCase.follower)
        yield obtained).assertEquals(testCase.expected)

object PostgresFollowersRepositorySuite:
  final private case class TestCase(
      expected: Boolean,
      followed: UserId,
      follower: UserId,
      followerRows: List[FollowerRow],
      userRows: List[UserRow],
  )

  private val testCaseGen = for
    case followed :: follower :: otherUsers <- uniqueTokenLessUsersWithId(7)
    allUsers = followed :: follower :: otherUsers
    following <- Gen.oneOf(true, false)
    followerRows = (if following then List(FollowerRow(followed.userId, follower.userId))
                    else List.empty) ++ otherUsers.map(x => FollowerRow(followed.userId, x.userId))
    userRows = allUsers.map:
      case UserWithId(userId, userWithPassword) =>
        val user = userWithPassword.user
        UserRow(
          userId,
          user.email,
          user.username,
          userWithPassword.password.value,
          user.bio,
          user.image.map(_.toString),
        )
  yield TestCase(following, followed.userId, follower.userId, followerRows, userRows)
