package vesting

import scalus.uplc.builtin.Data
import scalus.cardano.address.Address
import scalus.cardano.ledger.*
import scalus.cardano.txbuilder.*
import scalus.cardano.onchain.plutus.v1.PubKeyHash
import scalus.uplc.PlutusV3

import java.time.Instant

/** Off-chain transaction builders for the vesting contract. Provided as plumbing: the assessment
  * is about the validator and its tests, not about learning TxBuilder.
  *
  * Pattern adapted from Scalus's own HTLC example (`scalus.examples.htlc.HtlcTransactions`).
  */
case class VestingTransactions(
    env: CardanoInfo,
    contract: PlutusV3[Data => Unit]
) {
    private val scriptAddress: Address = contract.address(env.network)

    private val builder = TxBuilder(env)

    /** Lock `value` at the script with a vesting datum. */
    def lock(
        utxos: Utxos,
        value: Value,
        sponsor: Address,
        owner: AddrKeyHash,
        beneficiary: AddrKeyHash,
        deadline: Instant,
        signer: TransactionSigner
    ): Transaction = {
        val datum =
            VestingDatum(PubKeyHash(owner), PubKeyHash(beneficiary), deadline.toEpochMilli)

        builder
            .payTo(scriptAddress, value, datum)
            .complete(availableUtxos = utxos, sponsor = sponsor)
            .sign(signer)
            .transaction
    }

    /** Beneficiary claims the locked funds. The validity range starts at `validFrom`. */
    def claim(
        utxos: Utxos,
        lockedUtxo: Utxo,
        payeeAddress: Address,
        sponsor: Address,
        beneficiary: AddrKeyHash,
        validFrom: Instant,
        signer: TransactionSigner
    ): Transaction =
        builder
            .spend(lockedUtxo, VestingAction.Claim, contract)
            .requireSignature(beneficiary)
            .payTo(payeeAddress, lockedUtxo.output.value)
            .validFrom(validFrom)
            .complete(availableUtxos = utxos, sponsor)
            .sign(signer)
            .transaction

    /** Owner takes the funds back. The validity range ends at `validTo`. */
    def cancel(
        utxos: Utxos,
        lockedUtxo: Utxo,
        payeeAddress: Address,
        sponsor: Address,
        owner: AddrKeyHash,
        validTo: Instant,
        signer: TransactionSigner
    ): Transaction =
        builder
            .spend(lockedUtxo, VestingAction.Cancel, contract)
            .requireSignature(owner)
            .payTo(payeeAddress, lockedUtxo.output.value)
            .validTo(validTo)
            .complete(availableUtxos = utxos, sponsor)
            .sign(signer)
            .transaction
}
