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
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.util.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.math.BigInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class FFIWalletTests {

    private companion object {
        private lateinit var walletDir: String

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val walletMod = WalletModule()
            walletDir = walletMod.provideWalletFilesDirPath(context)
            clean()
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            val walletMod = WalletModule()
            FFITestUtil.printFFILogFile(
                walletMod.provideWalletLogFilePath(
                    walletMod.provideLogFilesDirPath(walletDir)
                )
            )
        }

        private fun clean() {
            val clean = FFITestUtil.clearTestFiles(walletDir)
            if (!clean) {
                throw RuntimeException("Test files could not cleared.")
            }
        }
    }

    @After
    fun tearDown() {
        clean()
    }

    @Test
    fun construction_assertThatValidInstanceWasCreated() {
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = TestListener()
        assertNotEquals(nullptr, wallet.getPointer())
        wallet.destroy()
        commsConfig.destroy()
        transport.destroy()
    }

    @Test
    fun getPublicKey_assertThatValidPublicKeyInstanceWasReturned() {
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = TestListener()
        assertNotEquals(nullptr, wallet.getPublicKey().getPointer())
        wallet.destroy()
        commsConfig.destroy()
        transport.destroy()
    }

    @Test
    fun verifyMessageSignature_assertThatSignedMessageVerificationWasSuccessful() {
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = TestListener()
        val message = "Hello"
        val signature = wallet.signMessage(message)
        assertTrue(wallet.verifyMessageSignature(wallet.getPublicKey(), message, signature))
        wallet.destroy()
        commsConfig.destroy()
        transport.destroy()
    }

    @Test
    fun onTxReceived_assertThatValidTxObjectWasGivenToTheListener() = runBlocking {
        val listener = mockk<FFIWalletListenerAdapter>(relaxed = true, relaxUnitFun = true)
        val receivedTxSlot = slot<PendingInboundTx>()
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = listener
        commsConfig.destroy()
        suspendWithTimeout<Unit>(2000L) { c ->
            every { listener.onTxReceived(capture(receivedTxSlot)) } answers { c.resume(Unit)  }
            assertTrue(wallet.testReceiveTx())
        }
        verify { listener.onTxReceived(any()) }
        wallet.destroy()
    }

    private suspend inline fun <T> suspendWithTimeout(
        timeoutMillis: Long,
        crossinline block: (Continuation<T>) -> Unit
    ) = withTimeout(timeoutMillis) { suspendCancellableCoroutine(block) }

    @Test
    @Ignore("Does not work currently due to failing wuth 999 code generateTestData function")
    fun testWallet() {
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = TestListener()
        commsConfig.destroy()

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
            if (wallet.getPublicKey().toString() == completedTx.getSourcePublicKey().toString()) {
                assertTrue(completedTx.isOutbound())
            } else {
                assertFalse(completedTx.isOutbound())
            }
            completedTx.destroy()
        }

        var wasRun = false
        // test pending inbound transactions
        assertTrue(wallet.testReceiveTx())
        val pendingInboundTxs = wallet.getPendingInboundTxs()
        assertTrue(pendingInboundTxs.getPointer() != nullptr)
        assertTrue(pendingInboundTxs.getLength() > 0)
        for (i in 0 until pendingInboundTxs.getLength()) {
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
            if (inbound.getStatus() == FFITxStatus.PENDING) {
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

        // destroy objects
        transport.destroy()
        wallet.destroy()

        // TODO test listeners
    }

    @Test
    fun testPartialBackup() {
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDir,
            Constants.Wallet.discoveryTimeoutSec
        )
        val wallet = FFIWallet(commsConfig, "")
        wallet.listenerAdapter = TestListener()
        val originalFile = File(walletDir, FFITestUtil.WALLET_DB_NAME)
        val backupDir = File(walletDir, "backup")
        backupDir.mkdir()
        val backupFile = File(backupDir, "backupfile.sqlite3")
        FFIUtil.doPartialBackup(originalFile.absolutePath, backupFile.absolutePath)
        assertTrue(backupFile.exists())
        transport.destroy()
        commsConfig.destroy()
        wallet.destroy()
    }

    class TestListener : FFIWalletListenerAdapter {

        override fun onBaseNodeSyncComplete(requestId: BigInteger, success: Boolean) {
            Logger.i(
                "Base Node Sync Complete :: request id %s success %s",
                requestId.toString(),
                success.toString()
            )
        }

        override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
            Logger.i("Tx Received :: pending inbound tx id %s", pendingInboundTx.id.toString())
        }

        override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
            Logger.i(
                "Tx Reply Received :: pending outbound tx id %s",
                pendingOutboundTx.id.toString()
            )
        }

        override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
            Logger.i(
                "Tx Finalized :: pending inbound tx id: %s",
                pendingInboundTx.id.toString()
            )
        }

        override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
            Logger.i(
                "Inbound tx Broadcast :: pending inbound tx id %s",
                pendingInboundTx.id.toString()
            )
        }

        override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
            Logger.i(
                "Outbound tx Broadcast :: pending outbound tx id %s",
                pendingOutboundTx.id.toString()
            )
        }

        override fun onTxMined(completedTx: CompletedTx) {
            Logger.i("Tx Mined :: completed tx id: %s", completedTx.id.toString())
        }

        override fun onTxCancelled(cancelledTx: CancelledTx) {
            TODO("Not yet implemented")
        }

        override fun onDirectSendResult(txId: BigInteger, success: Boolean) {
            Logger.i("Direct send :: tx id %s success %s", txId.toString(), success.toString())
        }

        override fun onStoreAndForwardSendResult(txId: BigInteger, success: Boolean) {
            Logger.i(
                "Store and forward :: tx id %s success %s",
                txId.toString(),
                success.toString()
            )
        }

    }

}
