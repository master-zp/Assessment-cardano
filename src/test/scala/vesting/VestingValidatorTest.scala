package vesting

import org.scalatest.funsuite.AnyFunSuite
import scalus.*
import scalus.cardano.ledger.ExUnits
import scalus.cardano.onchain.plutus.prelude
import scalus.uplc.builtin.{Builtins, Data}
import scalus.uplc.builtin.Data.toData

/** Unit tests: the compiled script is evaluated directly against hand-built script contexts. Fast,
  * no ledger, no emulator. Every rejection is checked for its REASON, not just for failing.
  *
  * Two tests are written for you, as a worked example. The rest are `pending`: replace each
  * `pending` with a real test body. Add more cases wherever you think the spec is not covered.
  */
class VestingValidatorTest extends AnyFunSuite {
    import VestingFixtures.*

    // `withErrorTraces` keeps the require(...) messages so that logs explain a rejection.
    private val script = VestingContract.compiled.withErrorTraces

    private def run(ctx: Data) = script(ctx).program.evaluateDebug

    private def assertAccepted(ctx: Data): ExUnits = {
        val result = run(ctx)
        assert(result.isSuccess, s"expected the validator to accept, but it failed: $result")
        result.budget
    }

    private def assertRejected(ctx: Data, reason: String): Unit = {
        val result = run(ctx)
        assert(!result.isSuccess, "expected the validator to reject, but it accepted")
        assert(
          result.logs.exists(_.contains(reason)),
          s"rejected for the wrong reason. expected: '$reason', logs: ${result.logs}"
        )
    }

    // ---- Worked examples (these fail until your validator is implemented) ----------------------

    test("claim: beneficiary, range starts exactly at the deadline -> accepted") {
        assertAccepted(claim(validFrom(deadline), signedBy(beneficiary)))
    }

    test("claim: range starts one millisecond before the deadline -> rejected") {
        assertRejected(
          claim(validFrom(deadline - 1), signedBy(beneficiary)),
          VestingValidator.TooEarlyToClaim
        )
    }

    // ---- Claim: TODO ---------------------------------------------------------------------------

    test("claim: signed by a stranger -> rejected") { pending }

    test("claim: no signatures -> rejected") { pending }

    test("claim: no validity range set -> rejected") { pending }

    // ---- Cancel: TODO --------------------------------------------------------------------------

    test("cancel: owner, range ends exactly at the deadline -> accepted") { pending }

    test("cancel: owner, range ends well before the deadline -> accepted") { pending }

    test("cancel: range ends after the deadline -> rejected") { pending }

    test("cancel: signed by the beneficiary (not the owner) -> rejected") { pending }

    test("cancel: no validity range set -> rejected") { pending }

    // ---- Think about the edges: TODO -----------------------------------------------------------
    // Hints: a validity range that straddles the deadline; a range with only one bound set;
    // several signers; a missing datum; a redeemer that is neither Claim nor Cancel.
    // Can any single time window be accepted for BOTH claim and cancel? Write a test for it.

    test("a validity range that straddles the deadline is rejected for both actions") { pending }

    test("missing datum -> rejected") { pending }

    // ---- Size and cost: TODO -------------------------------------------------------------------
    // Print the script size and the CPU/memory budget of a successful claim and cancel,
    // then copy the numbers into NOTES.md. Hint: `info(...)` prints in the test report;
    // `VestingContract.compiled.script.script.size` is the size in bytes.

    test("report script size and execution budget") { pending }
}
