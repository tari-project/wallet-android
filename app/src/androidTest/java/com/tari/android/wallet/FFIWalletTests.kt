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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.orhanobut.logger.Logger
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class FFIWalletTests {

    class TestListener : FFIWalletListenerAdapter {
        override fun onTxBroadcast(completedTx: CompletedTx) {
            Logger.i("Tx Broadcast :: completed tx id %s", completedTx.id.toString())
        }

        override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
            Logger.i("Tx Received :: pending inbound tx id %s", pendingInboundTx.id.toString())
        }

        override fun onTxReplyReceived(completedTx: CompletedTx) {
            Logger.i("Tx Reply Received :: completed tx id %s", completedTx.id.toString())
        }

        override fun onTxMined(completedTx: CompletedTx) {
            Logger.i("Tx Mined :: completed tx id: %s", completedTx.id.toString())
        }

        override fun onTxFinalized(completedTx: CompletedTx) {
            Logger.i("Tx Finalized :: completed tx id: %s", completedTx.id.toString())
        }

        override fun onDirectSendResult(txId: BigInteger, success: Boolean) {
            Logger.i("Direct send :: tx id %s success %s", txId.toString(), success.toString())
        }

        override fun onStoreAndForwardSendResult(txId: BigInteger, success: Boolean) {
            Logger.i("Store and forward :: tx id %s success %s", txId.toString(), success.toString())
        }

        override fun onTxCancellation(txId: BigInteger) {
            Logger.i("Tx cancellation :: tx id %s", txId.toString())
        }

        override fun onBaseNodeSyncComplete(rxId: BigInteger, success: Boolean) {
            Logger.i("Base Node Sync Complete :: request id %s success %s", rxId.toString(), success.toString())
        }
    }

    @Test
    fun testWallet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val walletMod = WalletModule()
        val clean = FFITestUtil.clearTestFiles(walletMod.provideWalletFilesDirPath(context))
        if (!clean) {
            throw RuntimeException("Test files could not cleared.")
        }

        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletMod.provideWalletFilesDirPath(context),
            FFIPrivateKey(HexString(FFITestUtil.PRIVATE_KEY_HEX_STRING)),
            Constants.Wallet.discoveryTimeoutSec
        )
        val listener = TestListener()
        val wallet = FFIWallet(commsConfig, "")
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
        assertTrue(wallet.generateTestData(walletMod.provideWalletFilesDirPath(context)))


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
        for (i in 0 until completedTxs.getLength()) {
            val completedTx = completedTxs.getAt(i)
            assertTrue(completedTx.getPointer() != nullptr)
            val completedID = completedTx.getId()
            completedID.toString()
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
        }

        var wasRun = false
        // test pending inbound transactions
        assertTrue(wallet.testReceiveTx())
        val pendingInboundTxs = wallet.getPendingInboundTxs()
        assertTrue(pendingInboundTxs.getPointer() != nullptr)
        assertTrue(pendingInboundTxs.getLength() > 0)
        for (i in 0 until pendingInboundTxs.getLength())
        {
            val inbound = pendingInboundTxs.getAt(i)
            assertTrue(inbound.getPointer() != nullptr)
            val inboundTxID = inbound.getId()
            inboundTxID.toString()
            val inboundTxSource = inbound.getSourcePublicKey()
            assertTrue(inboundTxSource.getPointer() != nullptr)
            val inboundTxAmount = inbound.getAmount()
            assertTrue(inboundTxAmount > BigInteger("0"))
            val inboundTxTimestamp = inbound.getTimestamp()
            inboundTxTimestamp.toString()
            if (inbound.getStatus() == FFITxStatus.PENDING)
            {
                val inboundTx = wallet.getPendingInboundTxById(inbound.getId())
                assertTrue(inboundTx.getPointer() != nullptr)
                assertTrue(wallet.testFinalizeReceivedTx(inboundTx))
                assertTrue(wallet.testBroadcastTx(inboundTx.getId()))
                assertTrue(wallet.testMineTx(inboundTx.getId()))
                inboundTx.destroy()
                wasRun = true
            }
            inbound.destroy()
        }
        pendingInboundTxs.destroy()
        assertTrue(wasRun)

        // test balances
        val available = wallet.getAvailableBalance()
        assertTrue(available.toString().toBigIntegerOrNull() != null)
        val pendingIn = wallet.getPendingIncomingBalance()
        assertTrue(pendingIn.toString().toBigIntegerOrNull() != null)
        val pendingOut = wallet.getPendingOutgoingBalance()
        assertTrue(pendingOut.toString().toBigIntegerOrNull() != null)

        // test send tari
        val txId = wallet.sendTx(
            contact.getPublicKey(),
            BigInteger.valueOf(1000000L),
            BigInteger.valueOf(100L),
            "Android Wallet"
        )
        assertTrue(txId > BigInteger("0"))

        // test pending outbound transactions
        val pendingOutboundTxs = wallet.getPendingOutboundTxs()
        assertTrue(pendingOutboundTxs.getPointer() != nullptr)
        assertTrue(pendingOutboundTxs.getLength() > 0)
        for (i in 0 until pendingOutboundTxs.getLength()) {
            val outboundTx = pendingOutboundTxs.getAt(i)
            assertTrue(outboundTx.getPointer() != nullptr)
            val outboundTxSource = outboundTx.getDestinationPublicKey()
            assertTrue(outboundTxSource.getPointer() != nullptr)
            val outboundTxAmount = outboundTx.getAmount()
            assertTrue(outboundTxAmount > BigInteger("0"))
            val outboundTxFee = outboundTx.getFee()
            assertTrue(outboundTxFee > BigInteger("0"))
            val outboundTxTimestamp = outboundTx.getTimestamp()
            outboundTxTimestamp.toString()
            val outboundTxStatus = outboundTx.getStatus()
            outboundTxStatus.toString()
            outboundTx.destroy()
        }
        pendingOutboundTxs.destroy()

        val outbound = wallet.getPendingOutboundTxById(txId)
        assertTrue(wallet.testCompleteSentTx(outbound))
        outbound.destroy()

        // destroy objects
        transport.destroy()
        contact.destroy()
        wallet.destroy()

        FFITestUtil.printFFILogFile(walletMod.provideWalletLogFilePath(walletMod.provideLogFilesDirPath(walletMod.provideWalletFilesDirPath(context))))

        // TODO test listeners
    }

}