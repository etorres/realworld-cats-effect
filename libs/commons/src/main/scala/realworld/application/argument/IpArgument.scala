package es.eriktorr
package realworld.application.argument

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Argument

trait IpArgument:
  given hostArgument: Argument[Host] = new Argument[Host]:
    override def read(string: String): ValidatedNel[String, Host] =
      Host.fromString(string).fold(s"Invalid host: $string".invalidNel)(_.validNel)

    override def defaultMetavar: String = "host"

  given portArgument: Argument[Port] = new Argument[Port]:
    override def read(string: String): ValidatedNel[String, Port] =
      Port.fromString(string).fold(s"Invalid port: $string".invalidNel)(_.validNel)

    override def defaultMetavar: String = "port"

object IpArgument extends IpArgument
