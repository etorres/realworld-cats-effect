package es.eriktorr
package realworld.users.core.domain

import realworld.spec.StringGenerators.alphaNumericStringBetween
import realworld.users.core.domain.Password.{CipherText, PlainText}

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

final class PasswordSuite extends ScalaCheckSuite:
  test("should check a hash with a plain text password"):
    val password = Password.unsafeFrom[PlainText]("secret")
    val hash = Password.unsafeFrom[CipherText](
      "$argon2id$v=19$m=12,t=20,p=2$Npakt5Ba9ecJ9YpHr+hHd5bOVIKAszyMBY2IKE6Db+Ks6DT9iwBMdRilzb1D6WgT7UwJoMZwubgeMnA7HH809Q$0grc2l5W++jegyTj6XXjMTeidfnxc9gnqTbhZUEX8KM",
    )
    assert(Password.check(password, hash))

  property("ciphering is verifiable"):
    forAll(alphaNumericStringBetween(3, 128)): text =>
      val password = Password.unsafeFrom[PlainText](text)
      val hash = Password.cipher(password)
      hash.map(Password.check(password, _)).isValid
