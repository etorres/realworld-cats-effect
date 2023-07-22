package es.eriktorr
package realworld.shared

import cats.effect.{IO, Resource}
import org.slf4j.LoggerFactory

import java.io.{ByteArrayOutputStream, OutputStream}
import scala.util.Using

trait ConsoleLogger

object ConsoleLogger:
  def resource: Resource[IO, ConsoleLogger] = Resource.eval:
    for
      logger <- IO.delay(LoggerFactory.getLogger(classOf[ConsoleLogger]).nn)
      consoleLogger = new ConsoleLogger {}
      _ <- IO.fromTry(
        Using
          .Manager { use =>
            val byteArrayOutputStream = ByteArrayOutputStream(1024)
            new OutputStream:
              override def write(byte: Int): Unit = byte match
                case '\n' =>
                  val line = byteArrayOutputStream.toString
                  byteArrayOutputStream.reset()
                  logger.info(line)
                case _ => byteArrayOutputStream.write(byte)
          }
          .map: outputStream =>
            Console.withErr(outputStream)(consoleLogger)
            Console.withOut(outputStream)(consoleLogger),
      )
    yield consoleLogger
