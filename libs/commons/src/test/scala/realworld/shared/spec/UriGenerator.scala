package es.eriktorr
package realworld.shared.spec

import realworld.shared.spec.StringGenerators.nonEmptyAlphaNumericStringGen

import org.scalacheck.Gen

import java.net.URI

object UriGenerator:
  def uriGen(domain: String = "https://www.example.com/"): Gen[URI] =
    nonEmptyAlphaNumericStringGen.map(suffix => URI(s"$domain$suffix"))
