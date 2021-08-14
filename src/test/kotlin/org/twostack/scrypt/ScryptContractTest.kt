package org.twostack.scrypt


import com.fasterxml.jackson.module.kotlin.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.script.Interpreter
import org.twostack.bitcoin4j.script.Script
import org.twostack.bitcoin4j.script.ScriptBuilder
import org.twostack.bitcoin4j.script.ScriptOpCodes
import org.twostack.bitcoin4j.transaction.*
import org.twostack.scrypt.dto.ScryptContractDto
import java.math.BigInteger

class ScryptContractTest {

private fun buildCreditingTransactionWithData(contractTemplate: String, satoshis: BigInteger, counterVal : Long): Transaction? {
    val credTx = Transaction()
    val unlockingScript = ScriptBuilder().number(0).number(0).build()
    val coinbaseUnlockBuilder = DefaultUnlockBuilder(unlockingScript)
    val prevTxnId = ByteArray(32)
    val coinbaseInput = TransactionInput(
        prevTxnId,
        0,
        TransactionInput.MAX_SEQ_NUMBER,
        coinbaseUnlockBuilder
    )
    credTx.addInput(coinbaseInput)

    val scriptPubKeyNoData = Script.fromAsmString(contractTemplate)

    //tack data onto end of scriptPubkey
    val bytes: ByteArray = ByteArray(4)
    Utils.uint32ToByteArrayLE(counterVal, bytes,0)
    val scriptPubKey = ScriptBuilder(scriptPubKeyNoData).op(ScriptOpCodes.OP_RETURN).data(bytes).build();
//    val scriptPubKey = ScriptBuilder(scriptWithData).data(bytes).build()

    val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(scriptPubKey)
    val output = TransactionOutput(satoshis, lockingScriptBuilder)
    credTx.addOutput(output)
    return credTx
}

private fun buildSpendingTransactionWithData(creditingTransaction: Transaction, scriptSig: Script, contractTemplate: String, counterVal: Long): Transaction? {
    val spendingTx = Transaction()
    val unlockingScriptBuilder: UnlockingScriptBuilder = DefaultUnlockBuilder(scriptSig)
    val input = TransactionInput(
        Utils.reverseBytes(creditingTransaction.transactionIdBytes),
        0,
        TransactionInput.MAX_SEQ_NUMBER,
        unlockingScriptBuilder
    )
    spendingTx.addInput(input)

//    val spendingScriptPubkey = Script.fromAsmString(contractTemplate)

    val scriptPubKeyNoData = Script.fromAsmString(contractTemplate)

    //tack data onto end of scriptPubkey
    val bytes: ByteArray = ByteArray(4)
    Utils.uint32ToByteArrayLE(counterVal, bytes, 0);
    val scriptPubKey = ScriptBuilder(scriptPubKeyNoData).op(ScriptOpCodes.OP_RETURN).data(bytes).build();
//    val scriptPubKey = ScriptBuilder().data(bytes).build()

    val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(scriptPubKey)
    val output = TransactionOutput(BigInteger.ONE, lockingScriptBuilder)
    spendingTx.addOutput(output)
    return spendingTx
}


private fun buildCreditingTransaction(contractTemplate: String, nValue: BigInteger): Transaction? {
    val credTx = Transaction()
        val unlockingScript = ScriptBuilder().number(0).number(0).build()
        val coinbaseUnlockBuilder = DefaultUnlockBuilder(unlockingScript)
        val prevTxnId = ByteArray(32)
        val coinbaseInput = TransactionInput(
            prevTxnId,
            0,
            TransactionInput.MAX_SEQ_NUMBER,
            coinbaseUnlockBuilder
        )
        credTx.addInput(coinbaseInput)

        val scriptPubKey = Script.fromAsmString(contractTemplate)

        val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(scriptPubKey)
        val output = TransactionOutput(nValue, lockingScriptBuilder)
        credTx.addOutput(output)
        return credTx
    }

    private fun buildSpendingTransaction(creditingTransaction: Transaction, scriptSig: Script, contractTemplate: String): Transaction? {
        val spendingTx = Transaction()
        val unlockingScriptBuilder: UnlockingScriptBuilder = DefaultUnlockBuilder(scriptSig)
        val input = TransactionInput(
            Utils.reverseBytes(creditingTransaction.transactionIdBytes),
            0,
            TransactionInput.MAX_SEQ_NUMBER,
            unlockingScriptBuilder
        )
        spendingTx.addInput(input)

        val spendingScriptPubkey = Script.fromAsmString(contractTemplate)

        val lockingScriptBuilder: LockingScriptBuilder = DefaultLockBuilder(spendingScriptPubkey)
        val output = TransactionOutput(BigInteger.ONE, lockingScriptBuilder)
        spendingTx.addOutput(output)
        return spendingTx
    }

    @Test
    fun `it can spend an OP_PUSHTX sCrypt script` (){

        val str = javaClass.getResource("/contracts/pushTxTest_release_desc.json").readText()

        val mapper = jacksonObjectMapper()
        val contract : ScryptContractDto = mapper.readValue<ScryptContractDto>(str)

        assertEquals(4, contract.version)

        val txCredit = buildCreditingTransaction(contract.asm, BigInteger.ONE)

        //assemble preimage >
        val txSpendForPreimage = buildSpendingTransaction(txCredit!!, ScriptBuilder().build(), contract.asm)
        val sigHashType = SigHashType.ALL.value or SigHashType.FORKID.value
        val creditingScriptPubKey = txCredit.outputs.get(0).script
        val txSpendingPreimage= SigHash().getSighashPreimage(txSpendForPreimage, sigHashType, 0, creditingScriptPubKey, BigInteger.ZERO)
        //assemble preimage <

        //create scriptSig with pre-image data
        val scriptSig: Script = ScriptBuilder().data(txSpendingPreimage).build();
        val txSpend = buildSpendingTransaction(txCredit, scriptSig, contract.asm)

        //setup the flags needed for script verification
        val verifyFlags = HashSet<Script.VerifyFlag>()
        verifyFlags.add(Script.VerifyFlag.SIGHASH_FORKID)
        verifyFlags.add(Script.VerifyFlag.UTXO_AFTER_GENESIS)

        val interp = Interpreter()

        assertDoesNotThrow {
            interp.correctlySpends(scriptSig, creditingScriptPubKey, txSpend, 0, verifyFlags)
        }
    }


    @Test
    fun `it can spend an OP_PUSHTX with a counter` (){

        val str = javaClass.getResource("/contracts/pushTxCounter_release_desc.json").readText()

        val mapper = jacksonObjectMapper()
        val contract : ScryptContractDto = mapper.readValue<ScryptContractDto>(str)

        assertEquals(4, contract.version)

        val creditingAsm = contract.asm
        val txCredit: Transaction? = buildCreditingTransactionWithData(creditingAsm, BigInteger.ONE, 2)
        val creditingScriptPubkey = txCredit?.outputs?.get(0)?.script

        //assemble preimage >
        val spendingAsm = contract.asm
        val spendingTxForPreimage= buildSpendingTransactionWithData(txCredit!!, ScriptBuilder().build(), spendingAsm, 3)
        val sigHashType = SigHashType.ALL.value or SigHashType.FORKID.value
        val spendingTxPreimage = SigHash().getSighashPreimage(spendingTxForPreimage, sigHashType, 0, creditingScriptPubkey, BigInteger.ZERO)
        //assemble preimage <

        //create scriptSig with pre-image data
        val scriptSig: Script = ScriptBuilder().data(spendingTxPreimage).build(); //preimage + satoshi value
        val txSpend = buildSpendingTransactionWithData(txCredit, scriptSig, spendingAsm, 3)

        //setup the flags needed for script verification
        val verifyFlags = HashSet<Script.VerifyFlag>()
        verifyFlags.add(Script.VerifyFlag.SIGHASH_FORKID)
        verifyFlags.add(Script.VerifyFlag.UTXO_AFTER_GENESIS)

        val interp = Interpreter()

        assertDoesNotThrow {
            interp.correctlySpends(scriptSig, creditingScriptPubkey, txSpend, 0, verifyFlags)
        }
    }

    @Test
    fun `it can spend an sCrypt contract` () {

        val str = javaClass.getResource("/contracts/sometest_release_desc.json").readText()

        val mapper = jacksonObjectMapper()
        val contract : ScryptContractDto = mapper.readValue<ScryptContractDto>(str)

        assertEquals(4, contract.version)

        val finalAsm = contract.asm.replace("\$data", "2")

        val scriptSig: Script = ScriptBuilder().number(2).build();

        val txCredit = buildCreditingTransaction(finalAsm, BigInteger.ZERO)
        val txSpend = buildSpendingTransaction(txCredit!!, scriptSig, finalAsm)

        val verifyFlags = HashSet<Script.VerifyFlag>()
        verifyFlags.add(Script.VerifyFlag.SIGHASH_FORKID)
        verifyFlags.add(Script.VerifyFlag.UTXO_AFTER_GENESIS)

        val interp = Interpreter()

        val creditingScriptPubkey = txCredit.outputs.get(0).script
        assertDoesNotThrow {
            interp.correctlySpends(scriptSig, creditingScriptPubkey, txSpend, 0, verifyFlags)
        }
    }

}
