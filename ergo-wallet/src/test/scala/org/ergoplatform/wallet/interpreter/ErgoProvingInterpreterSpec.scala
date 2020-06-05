package org.ergoplatform.wallet.interpreter

import org.ergoplatform.wallet.crypto.ErgoSignature
import org.ergoplatform.wallet.secrets.{DlogSecretKey, ExtendedSecretKey}
import org.ergoplatform.wallet.utils.Generators
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.util.Random

class ErgoProvingInterpreterSpec
  extends FlatSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with Generators
    with InterpreterSpecCommon {

  it should "produce proofs with primitive secrets" in {
    val entropy = Random.randomBytes(32)
    val extendedSecretKey = ExtendedSecretKey.deriveMasterKey(entropy)
    val fullProver = ErgoProvingInterpreter(extendedSecretKey, parameters)

    val primitiveKey = DlogSecretKey(extendedSecretKey.privateInput)
    val primitiveProver = ErgoProvingInterpreter(IndexedSeq(primitiveKey), parameters)

    forAll(unsignedTxGen(extendedSecretKey)) { case (ins, unsignedTx) =>
      val signedTxFull = fullProver.sign(unsignedTx, ins.toIndexedSeq, IndexedSeq(), stateContext).get
      val signedTxUnsafe = primitiveProver.sign(unsignedTx, ins.toIndexedSeq, IndexedSeq(), stateContext).get

      signedTxFull shouldEqual signedTxUnsafe

      signedTxFull.inputs.map(_.spendingProof.proof).zip(signedTxFull.inputs.map(_.spendingProof.proof))
        .foreach { case (fullProof, unsafeProof) =>
          ErgoSignature.verify(unsignedTx.messageToSign, fullProof, extendedSecretKey.publicKey.key.h) shouldBe
            ErgoSignature.verify(unsignedTx.messageToSign, unsafeProof, extendedSecretKey.publicKey.key.h)
        }
    }
  }

  it should "produce a signature with enough hints given - 2-out-of-3 case" in {
    def obtainSecretKey() = ExtendedSecretKey.deriveMasterKey(Random.randomBytes(32))

    val prover0 = ErgoProvingInterpreter(obtainSecretKey(), parameters)
    val prover1 = ErgoProvingInterpreter(obtainSecretKey(), parameters)
    val prover2 = ErgoProvingInterpreter(obtainSecretKey(), parameters)


  }
}
