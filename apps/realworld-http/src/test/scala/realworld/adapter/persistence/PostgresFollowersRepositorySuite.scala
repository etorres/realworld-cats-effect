package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresFollowersRepositorySuite.testCaseGen
import realworld.adapter.persistence.row.{FollowerRow, UserRow}
import realworld.domain.model.RealWorldGenerators.{uniqueUserKeys, userGen, userWithHashPasswordGen}
import realworld.domain.model.UserId
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.shared.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
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

  final private case class UserData(userId: UserId, userWithPassword: UserWithHashPassword)

  private val testCaseGen = for
    userKeys <- uniqueUserKeys(7)
    users <- userKeys.traverse: key =>
      for
        user <- userGen(emailGen = key.email, usernameGen = key.username)
        userWithHashPassword <- userWithHashPasswordGen(userGen = user)
      yield UserData(key.userId, userWithHashPassword)
    followed :: follower :: otherUserIds = userKeys.map(_.userId): @unchecked
    following <- Gen.oneOf(true, false)
    followerRows = (if following then List(FollowerRow(followed, follower))
                    else List.empty) ++ otherUserIds
      .map(userId => FollowerRow(followed, userId))
    userRows = users
      .map:
        case UserData(userId, userWithPassword) =>
          val user = userWithPassword.user
          UserRow(
            userId,
            user.email,
            user.username,
            userWithPassword.password.value,
            user.bio,
            user.image.map(_.toString),
          )
  yield TestCase(following, followed, follower, followerRows, userRows)
