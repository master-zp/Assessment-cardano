package vesting

import org.scalatest.funsuite.AnyFunSuite
import scalus.cardano.ledger.*
import scalus.cardano.node.Emulator
import scalus.testing.kit.Party.{Alice, Bob, Eve}
import scalus.testing.kit.{ScalusTest, TestUtil}
import scalus.utils.await

import java.time.Instant

/** End-to-end tests: real transactions are built, balanced, and submitted to Scalus's in-memory
  * emulator, which runs the full ledger rules including script evaluation and the validity
  * interval check against the emulator's current slot.
  *
  * Cast: Alice = owner, Bob = beneficiary, Eve = stranger.
  *
  * One test is written for you as a worked example; the rest are `pending`. The off-chain builders
  * (`VestingTransactions`) are provided: you are not being assessed on TxBuilder.
  */
class VestingEmulatorTest extends AnyFunSuite, ScalusTest {

    private given env: CardanoInfo = TestUtil.testEnvironment
    private val contract = VestingContract.compiled.withErrorTraces
    private val txs = VestingTransactions(env, contract)

    private val deadlineSlot: SlotNo = 10
    private val beforeSlot: SlotNo = deadlineSlot - 1
    private val afterSlot: SlotNo = deadlineSlot + 1
    private val deadline: Instant = env.slotConfig.slotToInstant(deadlineSlot)
    private val beforeDeadline: Instant = env.slotConfig.slotToInstant(beforeSlot)
    private val afterDeadline: Instant = env.slotConfig.slotToInstant(afterSlot)

    private def createProvider: Emulator =
        Emulator.withAddresses(Seq(Alice.address, Bob.address, Eve.address))

    /** Alice locks 10 ADA for Bob until `deadline`. Returns the locked UTxO. */
    private def lock(provider: Emulator): Utxo = {
        val utxos = provider.findUtxos(address = Alice.address).await().toOption.get

        val lockTx = txs.lock(
          utxos = utxos,
          value = Value.ada(10),
          sponsor = Alice.address,
          owner = Alice.addrKeyHash,
          beneficiary = Bob.addrKeyHash,
          deadline = deadline,
          signer = Alice.signer
        )
        assert(provider.submit(lockTx).await().isRight, "locking should succeed")

        val lockedUtxo = lockTx.utxos.find { case (_, txOut) =>
            txOut.address == contract.address(env.network)
        }.get
        Utxo(lockedUtxo)
    }

    // ---- Worked example (fails until your validator is implemented) ----------------------------

    test("beneficiary claims after the deadline") {
        val provider = createProvider
        val lockedUtxo = lock(provider)
        val utxos = provider.findUtxos(Bob.address).await().toOption.get

        val claimTx = txs.claim(
          utxos = utxos,
          lockedUtxo = lockedUtxo,
          payeeAddress = Bob.address,
          sponsor = Bob.address,
          beneficiary = Bob.addrKeyHash,
          validFrom = afterDeadline,
          signer = Bob.signer
        )

        // The emulator checks the validity interval against the current slot, so move the clock.
        provider.setSlot(afterSlot)
        val result = provider.submit(claimTx).await()
        assert(result.isRight, s"Emulator submission failed: $result")
    }

    // ---- TODO ----------------------------------------------------------------------------------
    // Hint for a rejection test: `assertScriptFail(<message>) { <build the transaction> }`
    // (from ScalusTest) expects building the transaction to fail in the script with that message.
    // `VestingValidator.TooEarlyToClaim` etc. are the messages. Use `beforeDeadline` /
    // `afterDeadline` for the validity bounds.

    test("beneficiary cannot claim before the deadline") { pending }

    test("a stranger cannot claim after the deadline") { pending }

    test("owner cancels before the deadline") { pending }

    test("owner cannot cancel after the deadline") { pending }

    test("a stranger cannot cancel before the deadline") { pending }
}
