package es.eriktorr
package realworld.application

import com.monovore.decline.Opts

final case class RealWorldParams(verbose: Boolean)

object RealWorldParams:
  def opts: Opts[RealWorldParams] = Opts
    .flag("verbose", short = "v", help = "Print extra metadata to the logs.")
    .orFalse
    .map(RealWorldParams.apply)
