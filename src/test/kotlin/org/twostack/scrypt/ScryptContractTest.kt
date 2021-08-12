package org.twostack.scrypt


import com.fasterxml.jackson.module.kotlin.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.twostack.bitcoin4j.Address
import org.twostack.bitcoin4j.PrivateKey
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.params.NetworkAddressType
import org.twostack.bitcoin4j.script.Interpreter
import org.twostack.bitcoin4j.script.Script
import org.twostack.bitcoin4j.script.ScriptBuilder
import org.twostack.bitcoin4j.transaction.*
import org.twostack.scrypt.dto.ScryptContractDto
import java.math.BigInteger

class ScryptContractTest {

    private fun buildCreditingTransaction(scriptPubKey: Script, nValue: BigInteger): Transaction? {
        val credTx = Transaction()
        val unlockingScript = ScriptBuilder().number(0).number(0).build()
        val coinbaseUnlockBuilder = DefaultUnlockBuilder(unlockingScript)
        val prevTxnId = ByteArray(32)
        val coinbaseInput = TransactionInput(
            prevTxnId,
            -0x1,
            TransactionInput.MAX_SEQ_NUMBER,
            coinbaseUnlockBuilder
        )
        credTx.addInput(coinbaseInput)
        val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(scriptPubKey)
        val output = TransactionOutput(nValue, lockingScriptBuilder)
        credTx.addOutput(output)
        return credTx
    }

    private fun buildSpendingTransaction(creditingTransaction: Transaction, scriptSig: Script): Transaction? {
        val spendingTx = Transaction()
        val unlockingScriptBuilder: UnlockingScriptBuilder = DefaultUnlockBuilder(scriptSig)
        val input = TransactionInput(
            Utils.reverseBytes(creditingTransaction.transactionIdBytes),
            0,
            TransactionInput.MAX_SEQ_NUMBER,
            unlockingScriptBuilder
        )
        spendingTx.addInput(input)
        val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(ScriptBuilder().build())
        val output = TransactionOutput(BigInteger.ZERO, lockingScriptBuilder)
        spendingTx.addOutput(output)
        return spendingTx
    }

    @Test
    fun `it can spend an OP_PUSHTX sCrypt script` (){

        val str = javaClass.getResource("/contracts/pushTxTest_release_desc.json").readText()

        val mapper = jacksonObjectMapper()
        val contract : ScryptContractDto = mapper.readValue<ScryptContractDto>(str)

        assertEquals(4, contract.version)

        val scriptPubKey = Script.fromAsmString(contract.asm)


        val txCredit = buildCreditingTransaction(scriptPubKey, BigInteger.ZERO)

        //assemble preimage >
        val txSpendForPreimage = buildSpendingTransaction(txCredit!!, ScriptBuilder().build())
        val sigHashType = SigHashType.ALL.value or SigHashType.FORKID.value
        val txCreditPreImage = SigHash().getSighashPreimage(txSpendForPreimage, sigHashType, 0, scriptPubKey, BigInteger.ZERO)
        //assemble preimage <

        //create scriptSig with pre-image data
        val scriptSig: Script = ScriptBuilder().data(txCreditPreImage).build();
        val txSpend = buildSpendingTransaction(txCredit, scriptSig)

        //setup the flags needed for script verification
        val verifyFlags = HashSet<Script.VerifyFlag>()
        verifyFlags.add(Script.VerifyFlag.SIGHASH_FORKID)
        verifyFlags.add(Script.VerifyFlag.UTXO_AFTER_GENESIS)

        val interp = Interpreter()

        assertDoesNotThrow {
            interp.correctlySpends(scriptSig, scriptPubKey, txSpend, 0, verifyFlags)
        }
    }

    @Test
    fun `it can spend an sCrypt contract` () {

        val str = javaClass.getResource("/contracts/sometest_release_desc.json").readText()

        val mapper = jacksonObjectMapper()
        val contract : ScryptContractDto = mapper.readValue<ScryptContractDto>(str)

        assertEquals(4, contract.version)

        val finalAsm = contract.asm.replace("\$data", "2")
        val scriptPubKey = Script.fromAsmString(finalAsm)

        val scriptSig: Script = ScriptBuilder().number(2).build();

        val txCredit = buildCreditingTransaction(scriptPubKey, BigInteger.ZERO)
        val txSpend = buildSpendingTransaction(txCredit!!, scriptSig)

        val verifyFlags = HashSet<Script.VerifyFlag>()
        verifyFlags.add(Script.VerifyFlag.SIGHASH_FORKID)
        verifyFlags.add(Script.VerifyFlag.UTXO_AFTER_GENESIS)

        val interp = Interpreter()

        assertDoesNotThrow {
            interp.correctlySpends(scriptSig, scriptPubKey, txSpend, 0, verifyFlags)
        }
    }

}
