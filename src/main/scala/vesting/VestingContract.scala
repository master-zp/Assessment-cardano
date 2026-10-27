package vesting

import scalus.compiler.Options
import scalus.uplc.PlutusV3

/** The validator compiled to a Plutus V3 script (Scala -> Scalus IR -> UPLC).
  *
  * `Options.release` gives the small production build. In tests we use
  * `VestingContract.compiled.withErrorTraces`, which keeps the `require` messages so that a
  * rejection can be checked for its reason.
  */
object VestingContract {
    private given Options = Options.release
    lazy val compiled = PlutusV3.compile(VestingValidator.validate)
}
