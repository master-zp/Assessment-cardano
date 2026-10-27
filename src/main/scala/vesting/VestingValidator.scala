package vesting

import scalus.compiler.Compile
import scalus.uplc.builtin.{Data, FromData, ToData}
import scalus.cardano.onchain.plutus.v3.*
import scalus.cardano.onchain.plutus.prelude.*

// =====================================================================================
//  YOUR TASK: implement the Claim and Cancel rules in `spend` below. See PROBLEM.md.
//  The datum, redeemer, entry point and error messages are given: keep their names,
//  the tests refer to them.
// =====================================================================================

/** Datum stored with every UTxO locked at the vesting script.
  *
  * @param owner
  *   may take the funds back (Cancel) while the lock is still closed
  * @param beneficiary
  *   may take the funds (Claim) once the lock has opened
  * @param deadline
  *   POSIX time in milliseconds at which the lock opens
  */
case class VestingDatum(
    owner: PubKeyHash,
    beneficiary: PubKeyHash,
    deadline: PosixTime
) derives FromData,
      ToData

/** Redeemer: what the spender wants to do with the locked UTxO. */
enum VestingAction derives FromData, ToData:
    case Claim
    case Cancel

@Compile
object VestingValidator {

    /** Entry point of the script: receives the whole ScriptContext as Data. */
    inline def validate(scData: Data): Unit = {
        val ctx = scData.to[ScriptContext]
        ctx.scriptInfo match
            case ScriptInfo.SpendingScript(txOutRef, datum) =>
                spend(datum, ctx.redeemer, ctx.txInfo, txOutRef)
            case _ => fail(MustBeSpending)
    }

    inline def spend(
        datum: Option[Data],
        redeemer: Data,
        tx: TxInfo,
        ownRef: TxOutRef
    ): Unit = {
        val vesting = datum.getOrFail(InvalidDatum).to[VestingDatum]
        redeemer.to[VestingAction] match
            case VestingAction.Claim =>
                // TODO: the rules for Claim (see PROBLEM.md, Part A)
                fail(NotImplemented)
            case VestingAction.Cancel =>
                // TODO: the rules for Cancel (see PROBLEM.md, Part A)
                fail(NotImplemented)
    }

    // Error messages. Tests assert on these, so a rejection is checked for the RIGHT reason.
    // Use each one in the place its name says.
    inline val NotImplemented = "TODO: not implemented"
    inline val MustBeSpending = "Must be a spending script"
    inline val InvalidDatum = "Invalid datum"
    inline val ValidRangeMustBeBound = "Validity range bound must be set"
    inline val TooEarlyToClaim = "Claim: validity range must start at or after the deadline"
    inline val UnsignedBeneficiary = "Claim: must be signed by the beneficiary"
    inline val TooLateToCancel = "Cancel: validity range must end at or before the deadline"
    inline val UnsignedOwner = "Cancel: must be signed by the owner"
}
