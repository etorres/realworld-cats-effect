package es.eriktorr
package realworld.users.profiles.domain

import realworld.users.core.domain.UserId

import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances

object FollowersGenerators:
  def followersGen(userIdsGen: Gen[List[UserId]]): Gen[Map[UserId, List[UserId]]] = for
    userIds <- userIdsGen
    followers <- userIds
      .flatTraverse(bob =>
        userIds
          .traverse(alice =>
            if bob != alice then Gen.frequency(1 -> Some(bob -> alice), 1 -> None)
            else Gen.const(None),
          ),
      )
      .map(_.collect { case Some(value) => value })
      .map(_.groupBy(_._1))
      .map(_.view.mapValues(_.map(_._2)).toMap)
  yield followers
