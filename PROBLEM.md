# Assessment: Time-Locked Vesting Validator (Scalus + Scala 3)

**Time box:** 1-2 hours. Stop at 2 hours and send what you have, with notes on what you would do next.
**Open book:** use the Scalus docs, examples and any other reference. This is not a memory test.
**Goal:** show that you can pick up the Cardano eUTxO model and Scalus quickly, write a correct validator, and test it well.

You do not need prior Cardano or Scalus experience. If you know UTXO chains, the new parts are the **datum** (data stored with a locked output), the **redeemer** (data supplied by whoever spends it) and the **script context** (the transaction, as the script sees it).

## Scenario

An owner locks some ADA at a script address for a beneficiary. Until a deadline, the owner can take the funds back. From the deadline on, only the beneficiary can take them.

## What is provided

| File | Status |
|---|---|
| `src/main/scala/vesting/VestingValidator.scala` | **You implement `spend`.** Datum, redeemer, entry point and error messages are given. |
| `src/main/scala/vesting/VestingContract.scala` | Given. Compiles the validator to Plutus V3. |
| `src/main/scala/vesting/VestingTransactions.scala` | Given. Off-chain `lock` / `claim` / `cancel` transaction builders. |
| `src/test/scala/vesting/VestingFixtures.scala` | Given. Builds hand-made script contexts for unit tests. |
| `src/test/scala/vesting/VestingValidatorTest.scala` | 2 worked tests, the rest `pending`. **You write the rest.** |
| `src/test/scala/vesting/VestingEmulatorTest.scala` | 1 worked test, the rest `pending`. **You write the rest.** |
| `NOTES.md` | **You fill this in.** |

`sbt test` should compile and show the worked tests failing (the validator is a stub). Your job is to turn everything green.

## Part A: the validator (required)

The datum and redeemer are already defined:

```scala
case class VestingDatum(owner: PubKeyHash, beneficiary: PubKeyHash, deadline: PosixTime)
enum VestingAction { case Claim; case Cancel }
```

`deadline` is POSIX time in **milliseconds**.

| Redeemer | The transaction is valid only if |
|---|---|
| `Claim` | it is signed by `beneficiary`, **and** its validity range starts at or after `deadline` |
| `Cancel` | it is signed by `owner`, **and** its validity range ends at or before `deadline` |
| anything else | it fails |

Also required:

- The script is only valid for **spending**.
- The datum must be present and decode as a `VestingDatum`.
- If the bound you need (lower for `Claim`, upper for `Cancel`) is not set, the transaction fails. A transaction with no validity range proves nothing about the time.
- Use the provided error constants (`TooEarlyToClaim`, `UnsignedOwner`, ...) so tests can check the reason.

A script cannot read the clock. It can read the interval in which the ledger promises the transaction is valid. If that interval starts at or after the deadline, then "it is at least the deadline" is guaranteed whenever the transaction is accepted. Check the Scalus docs and examples for how the validity range is exposed on `TxInfo`, and for which bounds are inclusive and which are exclusive.

## Part B: tests (required, the most important part)

Replace every `pending` with a real test. At minimum, cover:

**Unit tests (pure script context, `VestingValidatorTest`)**

1. Claim by the beneficiary at the deadline: accepted.
2. Claim before the deadline: rejected, for the right reason.
3. Claim signed by the wrong key / by nobody: rejected, for the right reason.
4. Cancel by the owner before the deadline: accepted. Include the exact boundary.
5. Cancel after the deadline: rejected, for the right reason.
6. Cancel signed by the wrong key: rejected, for the right reason.
7. A transaction with no validity range set (for both actions): rejected, for the right reason.
8. A validity range that straddles the deadline: rejected for both actions.
9. Missing datum: rejected.

**End-to-end tests (emulator, `VestingEmulatorTest`)**

10. Owner locks, beneficiary claims after the deadline.
11. Owner locks, owner cancels before the deadline.
12. The rejection cases: claim too early, claim by a stranger, cancel too late, cancel by a stranger.

A test that only checks "it failed" is a weak test: a bug elsewhere could make it fail for the wrong reason. Assert the reason.

## Part C: size and cost (required)

Fill in the table in `NOTES.md`: compiled script size (release build) and the CPU / memory budget of a successful claim and a successful cancel.

## Part D: short written answers (required, in `NOTES.md`)

1. What was hardest to learn, and what did you read or try to get past it?
2. Name **two** weaknesses, risks or limits of this contract design. Say how you would address each.
3. Suppose `Claim` must also guarantee that the beneficiary actually *receives* the locked amount. How would you check that on-chain? Then: what is a **double satisfaction** attack, and could it affect that check?

## Part E: stretch (optional, only if time remains)

Implement the payout check from question 3 in a **separate** validator object (do not break Parts A-C), and test it with the emulator: a claim that pays the beneficiary passes, a claim that pays someone else fails. If you run out of time, a written design in `NOTES.md` is fine.

## Deliverables

- The project (git repo or zip) with your validator and tests. Please do not commit `target/`.
- `NOTES.md` completed.
- `sbt test` output, or a note of what does not pass and why.

## Running

```
sbt test                              # everything
sbt "testOnly vesting.VestingValidatorTest"
sbt "testOnly vesting.VestingEmulatorTest"
```

Requirements: JDK 17 or newer, and sbt. The first run downloads dependencies and compiles, which can take a few minutes.

## Rules for the Scalus subset

Validators are a **subset** of Scala that compiles to Plutus Core. In `@Compile` code: use `val` and `def`, case classes, enums, pattern matching and `if`. No `var`, no `while`, no `try/catch`, no Scala standard-library collections (use Scalus's `List` and `Option` from the prelude, already imported). Start from the provided stub and keep its style.

## Getting unstuck

- Docs: https://scalus.org/docs (start with "Validators in depth" and the HTLC tutorial)
- Example contracts and tests: https://github.com/scalus3/scalus/tree/master/scalus-examples
- If you are blocked on **tooling or setup** for more than 20 minutes, send a short question instead of spending your time budget. Setup problems are not part of the assessment.
