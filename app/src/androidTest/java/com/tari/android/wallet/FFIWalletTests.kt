/**
 * Copyright 2020 The Tari Project
 *
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet

import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class FFIWalletTests {

    class TestListener : FFIWalletListenerAdapter {
        override fun onTxBroadcast(completedTxId: BigInteger) {
            Logger.i("Tx Broadcast :: completed tx id %s", completedTxId.toString())
        }

        override fun onTxReceived(pendingInboundTxId: BigInteger) {
            Logger.i("Tx Received :: pending inbound tx id %s", pendingInboundTxId.toString())
        }

        override fun onTxReplyReceived(completedTxId: BigInteger) {
            Logger.i("Tx Reply Received :: completed tx id %s", completedTxId.toString())
        }

        override fun onTxMined(completedTxId: BigInteger) {
            Logger.i("Tx Mined :: completed tx id: %s", completedTxId.toString())
        }

        override fun onTxFinalized(completedTxId: BigInteger) {
            Logger.i("Tx Finalized :: completed tx id: %s", completedTxId.toString())
        }

        override fun onDiscoveryComplete(txId: BigInteger, success: Boolean) {
            Logger.i("Discovery Complete :: tx id %s success %s", txId.toString(), success.toString())
        }
    }

    @Test
    fun testWallet() {
        val m = FFITestUtil.clearTestFiles(FFITestUtil.WALLET_DATASTORE_PATH)
        if (!m) {
            throw RuntimeException("Test files could not cleared.")
        }

        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            FFITestUtil.WALLET_CONTROL_SERVICE_ADDRESS,
            transport,
            FFITestUtil.WALLET_DB_NAME,
            FFITestUtil.WALLET_DATASTORE_PATH,
            FFIPrivateKey(HexString(FFITestUtil.PRIVATE_KEY_HEX_STRING))
        )
        val listener = TestListener()
        val wallet = FFITestWallet(commsConfig, FFITestUtil.WALLET_LOG_FILE_PATH)
        wallet.listenerAdapter = listener
        assertTrue(wallet.getPointer() != nullptr)
        commsConfig.destroy()

        // test get public key
        val pk = wallet.getPublicKey()
        assertTrue(pk.getPointer() != nullptr)

        //test sign and verify message
        val message = "Hello"
        val signature = wallet.signMessage(message)
        val verified = wallet.verifyMessageSignature(pk,message,signature)
        assertTrue(verified)

        // test data generation
        assertTrue(wallet.generateTestData(FFITestUtil.WALLET_DATASTORE_PATH))


        // test contacts
        val contacts = wallet.getContacts()
        assertTrue(contacts.getPointer() != nullptr)
        val length = contacts.getLength()
        assertTrue(length > 0)
        val contact = contacts.getAt(0)
        assertTrue(contact.getPointer() != nullptr)
        val removed = wallet.removeContact(contact)
        assertTrue(removed)
        val added = wallet.addUpdateContact(contact)
        assertTrue(added)

        // test completed transactions
        val completedTxs = wallet.getCompletedTxs()
        assertTrue(completedTxs.getPointer() != nullptr)
        assertTrue(completedTxs.getLength() > 0)
        var completedTx: FFICompletedTx? = null
        for (i in 0..completedTxs.getLength()) {
            completedTx = completedTxs.getAt(0)
            assertTrue(completedTx.getPointer() != nullptr)
            if (completedTx.getStatus() == FFICompletedTx.Status.MINED) {
                completedTx.destroy()
            } else {
                break
            }
        }
        val completedID = completedTx!!.getId()
        val completedTxSource = completedTx.getSourcePublicKey()
        assertTrue(completedTxSource.getPointer() != nullptr)
        completedTxSource.destroy()
        val completedTxDestination = completedTx.getDestinationPublicKey()
        assertTrue(completedTxDestination.getPointer() != nullptr)
        completedTxDestination.destroy()
        val completedTxAmount = completedTx.getAmount()
        assertTrue(completedTxAmount > BigInteger("0"))
        val completedTxFee = completedTx.getFee()
        assertTrue(completedTxFee > BigInteger("0"))
        val completedTxTimestamp = completedTx.getTimestamp()
        completedTxTimestamp.toString()
        val completedTxIsOutbound = wallet.isCompletedTxOutbound(completedTx)
        if (wallet.getPublicKey().toString() == completedTx.getSourcePublicKey().toString())
        {
            assertTrue(completedTxIsOutbound)
        } else
        {
            assertFalse(completedTxIsOutbound)
        }

        completedTx.destroy()
        completedTx = wallet.getCompletedTxById(completedID)
        assertTrue(completedTx.getPointer() != nullptr)
        assertTrue(wallet.testBroadcastTx(completedTx))
        assertTrue(wallet.testMineCompletedTx(completedTx))
        completedTx.destroy()
        completedTxs.destroy()

        // test pending inbound transactions
        assertTrue(wallet.testReceiveTx())
        val pendingInboundTxs = wallet.getPendingInboundTxs()
        assertTrue(pendingInboundTxs.getPointer() != nullptr)
        assertTrue(pendingInboundTxs.getLength() > 0)
        var inbound = pendingInboundTxs.getAt(0)
        assertTrue(inbound.getPointer() != nullptr)
        val inboundTxID = inbound.getId()
        val inboundTxSource = inbound.getSourcePublicKey()
        assertTrue(inboundTxSource.getPointer() != nullptr)
        val inboundTxAmount = inbound.getAmount()
        assertTrue(inboundTxAmount > BigInteger("0"))
        val inboundTxTimestamp = inbound.getTimestamp()
        inboundTxTimestamp.toString()
        inbound.destroy()
        inbound = wallet.getPendingInboundTxById(inboundTxID)
        assertTrue(inbound.getPointer() != nullptr)
        assertTrue(wallet.testFinalizeReceivedTx(inbound))
        inbound.destroy()
        pendingInboundTxs.destroy()

        // test pending outbound transactions
        val pendingOutboundTxs = wallet.getPendingOutboundTxs()
        assertTrue(pendingOutboundTxs.getPointer() != nullptr)
        assertTrue(pendingOutboundTxs.getLength() > 0)
        var outboundTx = pendingOutboundTxs.getAt(0)
        assertTrue(outboundTx.getPointer() != nullptr)
        val outboundTxID = outboundTx.getId()
        val outboundTxSource = outboundTx.getDestinationPublicKey()
        assertTrue(outboundTxSource.getPointer() != nullptr)
        val outboundTxAmount = outboundTx.getAmount()
        assertTrue(outboundTxAmount > BigInteger("0"))
        val outboundTxFee = outboundTx.getFee()
        assertTrue(outboundTxFee > BigInteger("0"))
        val outboundTxTimestamp = outboundTx.getTimestamp()
        outboundTxTimestamp.toString()
        outboundTx.destroy()
        outboundTx = wallet.getPendingOutboundTxById(outboundTxID)
        assertTrue(outboundTx.getPointer() != nullptr)
        assertTrue(wallet.testCompleteSentTx(outboundTx))
        outboundTx.destroy()
        pendingOutboundTxs.destroy()

        // test balances
        val available = wallet.getAvailableBalance()
        assertTrue(available.toString().toBigIntegerOrNull() != null)
        val pendingIn = wallet.getPendingIncomingBalance()
        assertTrue(pendingIn.toString().toBigIntegerOrNull() != null)
        val pendingOut = wallet.getPendingOutgoingBalance()
        assertTrue(pendingOut.toString().toBigIntegerOrNull() != null)

        // test send tari
        assertTrue(
            wallet.sendTx(
                contact.getPublicKey(),
                BigInteger.valueOf(1000000L),
                BigInteger.valueOf(100L),
                "Android Wallet"
            )
        )

        // destroy objects
        contact.destroy()
        wallet.destroy()

        FFITestUtil.printFFILogFile()

        // TODO test listeners
    }

}