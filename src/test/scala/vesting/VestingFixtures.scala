package vesting

import scalus.cardano.onchain.plutus.prelude
import scalus.cardano.onchain.plutus.v1.{IntervalBound, IntervalBoundType, PubKeyHash}
import scalus.cardano.onchain.plutus.v3.{Interval, ScriptContext, ScriptInfo, TxId, TxInfo, TxOutRef}
import scalus.uplc.builtin.{ByteString, Data}
import scalus.uplc.builtin.Data.toData

/** Plumbing for the pure (no emulator) unit tests: fixed keys, validity ranges and a builder that
  * produces a Plutus V3 spending `ScriptContext` as `Data`.
  *
  * Provided as part of the assessment so that time goes into the validator and the test cases, not
  * into assembling a ScriptContext by hand. Adapted from Scalus's HtlcVerificationTest.
  */
object VestingFixtures {

    private def key(byteHex: String): PubKeyHash =
        PubKeyHash(ByteString.fromHex(byteHex * 28)) // a pub key hash is 28 bytes

    val owner: PubKeyHash = key("11")
    val beneficiary: PubKeyHash = key("22")
    val stranger: PubKeyHash = key("33")

    /** The lock opens at this POSIX time (milliseconds). */
    val deadline: BigInt = BigInt(1000000)

    val datum: VestingDatum = VestingDatum(owner, beneficiary, deadline)

    private val ownRef: TxOutRef = TxOutRef(TxId(ByteString.fromHex("44" * 32)), 0)
    private val txId: TxId = TxId(ByteString.fromHex("55" * 32))

    // ---- validity ranges, written the way the ledger encodes them ---------------------------
    // lower bound: inclusive. upper bound: exclusive.

    /** [time, +infinity) */
    def validFrom(time: BigInt): Interval =
        Interval(IntervalBound(IntervalBoundType.Finite(time), true), IntervalBound.posInf)

    /** (-infinity, time) */
    def validUntil(time: BigInt): Interval =
        Interval(IntervalBound.negInf, IntervalBound(IntervalBoundType.Finite(time), false))

    /** [from, until) */
    def validBetween(from: BigInt, until: BigInt): Interval =
        Interval(
          IntervalBound(IntervalBoundType.Finite(from), true),
          IntervalBound(IntervalBoundType.Finite(until), false)
        )

    /** (-infinity, +infinity): the transaction has no validity range set. */
    val unbounded: Interval = Interval(IntervalBound.negInf, IntervalBound.posInf)

    // ---- signatories --------------------------------------------------------------------------

    val nobody: prelude.List[PubKeyHash] = prelude.List.empty[PubKeyHash]

    def signedBy(keys: PubKeyHash*): prelude.List[PubKeyHash] =
        keys.foldRight(prelude.List.empty[PubKeyHash])((k, rest) => prelude.List.Cons(k, rest))

    // ---- script contexts ----------------------------------------------------------------------

    /** A spending ScriptContext for the locked UTxO, as `Data` (what the script receives). */
    def spending(
        redeemer: Data,
        range: Interval,
        signers: prelude.List[PubKeyHash],
        lockedDatum: prelude.Option[Data] = prelude.Option.Some(datum.toData)
    ): Data =
        ScriptContext(
          TxInfo(
            inputs = prelude.List.Nil,
            validRange = range,
            signatories = signers,
            id = txId
          ),
          redeemer,
          ScriptInfo.SpendingScript(ownRef, lockedDatum)
        ).toData

    def claim(range: Interval, signers: prelude.List[PubKeyHash]): Data =
        spending(VestingAction.Claim.toData, range, signers)

    def cancel(range: Interval, signers: prelude.List[PubKeyHash]): Data =
        spending(VestingAction.Cancel.toData, range, signers)
}
