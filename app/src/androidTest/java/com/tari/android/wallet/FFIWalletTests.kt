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
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.util.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.math.BigInteger

class FFIWalletTests {

    private lateinit var wallet: FFIWallet
    private lateinit var listener: TestListener
    private val walletDirPath =
        ApplicationProvider.getApplicationContext<Context>().filesDir.absolutePath

    private fun clean() {
        val clean = FFITestUtil.clearTestFiles(walletDirPath)
        if (!clean) {
            throw RuntimeException("Test files could not cleared.")
        }
    }

    @Before
    fun setup() {
        // clean any existing wallet data
        clean()
        // create memory transport
        val transport = FFITransportType()
        // create comms config
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDirPath,
            Constants.Wallet.discoveryTimeoutSec
        )
        val logFile = File(walletDirPath, "test_log.log")
        // create wallet instance
        wallet = FFIWallet(commsConfig, logFile.absolutePath)
        // create listener
        listener = TestListener()
        wallet.listener = listener
        commsConfig.destroy()
        transport.destroy()
    }

    @After
    fun teardown() {
        // destroy wallet
        wallet.listener = null
        wallet.destroy()
        // clean wallet folder
        clean()
    }

    @Test
    fun validInstanceWithValidPublicKeyWasCreated() {
        assertNotEquals(nullptr, wallet.pointer)
        val publicKey = wallet.getPublicKey()
        assertNotEquals(nullptr, publicKey.pointer)
        assertEquals(
            FFITestUtil.PUBLIC_KEY_HEX_STRING.length,
            publicKey.toString().length
        )
    }

    @Test
    fun signedMessageVerificationWasSuccessful() {
        val message = "Hello"
        val signature = wallet.signMessage(message)
        assertTrue(wallet.verifyMessageSignature(wallet.getPublicKey(), message, signature))
    }

    @Test
    fun testContacts() {
        val contactCount = 127
        // add contacts
        repeat(contactCount) {
            val contactPrivateKey = FFIPrivateKey.generate()
            val contactPublicKey = FFIPublicKey(contactPrivateKey)
            val contact = FFIContact(
                FFITestUtil.generateRandomAlphanumericString(16),
                contactPublicKey
            )
            wallet.addUpdateContact(contact)
            contactPrivateKey.destroy()
            contactPublicKey.destroy()
            contact.destroy()
        }
        // test get contacts
        var contacts = wallet.getContacts()
        assertEquals(contactCount, contacts.getLength())
        // test update alias
        val lastContactOld = contacts.getAt(contactCount - 1)
        val lastContactOldPublicKey = lastContactOld.getPublicKey()
        val newAlias = FFITestUtil.generateRandomAlphanumericString(7)
        val lastContactNew = FFIContact(
            newAlias,
            lastContactOldPublicKey
        )
        lastContactOldPublicKey.destroy()
        wallet.addUpdateContact(lastContactNew)
        lastContactOld.destroy()
        lastContactNew.destroy()
        contacts.destroy()
        // re-fetch contacts
        contacts = wallet.getContacts()
        val lastContactUpdated = contacts.getAt(contactCount - 1)
        assertEquals(
            newAlias,
            lastContactUpdated.getAlias()
        )
        // test remove
        wallet.removeContact(lastContactUpdated)
        lastContactUpdated.destroy()
        contacts.destroy()
        contacts = wallet.getContacts()
        assertEquals(
            contactCount - 1,
            contacts.getLength()
        )
        contacts.destroy()
    }

    @Test
    fun testPartialBackup() {
        val originalFile = File(walletDirPath, FFITestUtil.WALLET_DB_NAME_WITH_EXTENSION)
        assertTrue(originalFile.exists())
        val backupDir = File(walletDirPath, "backup")
        backupDir.mkdir()
        val backupFile = File(backupDir, "backupfile.sqlite3")
        FFIUtil.doPartialBackup(originalFile.absolutePath, backupFile.absolutePath)
        assertTrue(backupFile.exists())
    }

    @Test
    fun testSeedWords() {
        val seedWords = wallet.getSeedWords()
        assertTrue(seedWords.getLength() > 0)
        assertTrue(seedWords.getAt(0).isNotEmpty())
        seedWords.destroy()
        assertEquals(nullptr, seedWords.pointer)
    }

    @Test
    fun testEmojiSet() {
        val emojiSet = FFIEmojiSet()
        assertTrue(emojiSet.getLength() > 0)
        val emoji = emojiSet.getAt(0)
        assertTrue(emoji.toString().isNotEmpty())
        emoji.destroy()
        emojiSet.destroy()
        assertEquals(nullptr, emojiSet.pointer)
    }

    @Test
    fun testBaseNodeFunctions() {
        val baseNodePublicKey = FFIPublicKey(
            HexString("06e98e9c5eb52bd504836edec1878eccf12eb9f26a5fe5ec0e279423156e657a")
        )
        val baseNodeAddress =
            "/onion3/bsmuof2cn4y2ysz253gzsvg3s72fcgh4f3qcm3hdlxdtcwe6al2dicyd:18141"

        val mockListener = mockk<FFIWalletListener>(relaxed = true, relaxUnitFun = true)
        val responseIds = mutableListOf<BigInteger>()
        every {
            mockListener.onBaseNodeSyncComplete(capture(responseIds), any())
        } answers { }
        wallet.listener = mockListener
        wallet.addBaseNodePeer(baseNodePublicKey, baseNodeAddress)
        baseNodePublicKey.destroy()
        val requestId = wallet.syncWithBaseNode()
        assertTrue(requestId > BigInteger("0"))
        Thread.sleep(5000)
        verify { mockListener.onBaseNodeSyncComplete(requestId, any()) }
        assertTrue(responseIds.contains(requestId))
    }

    @Test
    fun testReceiveTxFlow() {
        val mockListener = mockk<FFIWalletListener>(relaxed = true, relaxUnitFun = true)
        val receivedTxSlot = slot<PendingInboundTx>()
        every { mockListener.onTxReceived(capture(receivedTxSlot)) } answers { }
        wallet.listener = mockListener
        // receive tx
        assertTrue(wallet.testReceiveTx())
        Thread.sleep(1000)
        verify { mockListener.onTxReceived(any()) }
        val pendingInboundTx = receivedTxSlot.captured
        val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
        assertEquals(1, pendingInboundTxsFFI.getLength())
        val pendingInboundTxFFI = pendingInboundTxsFFI.getAt(0)
        assertEquals(
            pendingInboundTx.id,
            pendingInboundTxFFI.getId()
        )
        assertEquals(
            TxStatus.PENDING,
            pendingInboundTx.status
        )
        pendingInboundTxsFFI.destroy()
        // test get pending inbound tx by id
        val pendingInboundTxByIdFFI = wallet.getPendingInboundTxById(pendingInboundTx.id)
        assertEquals(
            pendingInboundTx.id,
            pendingInboundTxByIdFFI.getId()
        )
        pendingInboundTxByIdFFI.destroy()

        // test finalize
        val finalizedTxSlot = slot<PendingInboundTx>()
        every { mockListener.onTxFinalized(capture(finalizedTxSlot)) } answers { }
        assertTrue(wallet.testFinalizeReceivedTx(pendingInboundTxFFI))
        Thread.sleep(1000)
        verify { mockListener.onTxFinalized(any()) }
        val finalizedTx = finalizedTxSlot.captured
        assertEquals(
            pendingInboundTx.id,
            finalizedTx.id
        )
        assertEquals(
            TxStatus.COMPLETED,
            finalizedTx.status
        )
        pendingInboundTxFFI.destroy()

        // test broadcast
        val broadcastTxSlot = slot<PendingInboundTx>()
        every { mockListener.onInboundTxBroadcast(capture(broadcastTxSlot)) } answers { }
        assertTrue(wallet.testBroadcastTx(pendingInboundTx.id))
        Thread.sleep(1000)
        verify { mockListener.onInboundTxBroadcast(any()) }
        val broadcastTx = broadcastTxSlot.captured
        assertEquals(
            pendingInboundTx.id,
            broadcastTx.id
        )
        // test wallet pending inbound balance
        assertEquals(
            pendingInboundTx.amount.value,
            wallet.getPendingInboundBalance()
        )

        // test mine tx
        val minedTxSlot = slot<CompletedTx>()
        every { mockListener.onTxMined(capture(minedTxSlot)) } answers { }
        assertTrue(wallet.testMineTx(pendingInboundTx.id))
        Thread.sleep(1000)
        verify { mockListener.onTxMined(any()) }
        val minedTx = minedTxSlot.captured
        assertEquals(
            pendingInboundTx.id,
            minedTx.id
        )
        assertEquals(
            TxStatus.MINED,
            minedTx.status
        )
        // get completed txs
        val completedTxsFFI = wallet.getCompletedTxs()
        assertEquals(1, completedTxsFFI.getLength())
        val completedTxFFI = completedTxsFFI.getAt(0)
        assertEquals(
            pendingInboundTx.id,
            completedTxFFI.getId()
        )
        completedTxFFI.destroy()
        completedTxsFFI.destroy()
        // test get by id
        val minedTxByIdFFI = wallet.getCompletedTxById(pendingInboundTx.id)
        assertEquals(
            pendingInboundTx.id,
            minedTxByIdFFI.getId()
        )
        minedTxByIdFFI.destroy()
        // available balance
        assertEquals(
            pendingInboundTx.amount.value,
            wallet.getAvailableBalance()
        )
    }

    @Test
    fun testCancelCompleteAndBroadcastAndGetByIds() {
        Logger.i("Will generate test data.")
        assertTrue(wallet.generateTestData(walletDirPath))
        Logger.i("Test data generation completed.")
        val mockListener = mockk<FFIWalletListener>(relaxed = true, relaxUnitFun = true)
        wallet.listener = mockListener

        // get a pending tx to cancel -
        // there's no pending outbound tx in the generated test data, so pick an incoming tx
        assertTrue(wallet.testReceiveTx())
        val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
        var pendingInboundTxFFI: FFIPendingInboundTx? = null
        for(i in 0 until pendingInboundTxsFFI.getLength()) {
            pendingInboundTxFFI = pendingInboundTxsFFI.getAt(i)
            if (pendingInboundTxFFI.getStatus() == FFITxStatus.PENDING) {
                break
            }
            pendingInboundTxFFI.destroy()
        }
        pendingInboundTxsFFI.destroy()
        assertNotNull(pendingInboundTxFFI)
        // cancel tx
        val cancelledTxSlot = slot<CancelledTx>()
        every { mockListener.onTxCancelled(capture(cancelledTxSlot)) } answers { }
        assertTrue(wallet.cancelPendingTx(pendingInboundTxFFI!!.getId()))
        pendingInboundTxFFI.destroy()
        Thread.sleep(1000)
        verify { mockListener.onTxCancelled(any()) }
        val cancelledTx = cancelledTxSlot.captured
        val cancelledTxsFFI = wallet.getCancelledTxs()
        assertEquals(1, cancelledTxsFFI.getLength())
        val cancelledTxFFI = cancelledTxsFFI.getAt(0)
        cancelledTxsFFI.destroy()
        assertEquals(
            cancelledTx.id,
            cancelledTxFFI.getId()
        )
        cancelledTxFFI.destroy()
        // get by id
        val cancelledTxByIdFFI = wallet.getCancelledTxById(cancelledTx.id)
        assertEquals(
            cancelledTx.id,
            cancelledTxByIdFFI.getId()
        )
        cancelledTxByIdFFI.destroy()

        // pick a completed outbound tx
        val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
        var pendingOutboundTxFFI: FFIPendingOutboundTx? = null
        for(i in 0 until pendingOutboundTxsFFI.getLength()) {
            pendingOutboundTxFFI = pendingOutboundTxsFFI.getAt(i)
            if (pendingOutboundTxFFI.getStatus() == FFITxStatus.COMPLETED) {
                break
            }
            pendingOutboundTxFFI.destroy()
        }
        assertNotNull(pendingOutboundTxFFI)
        // broadcast tx
        val broadcastTxSlot = slot<PendingOutboundTx>()
        every { mockListener.onOutboundTxBroadcast(capture(broadcastTxSlot)) } answers { }
        assertTrue(wallet.testBroadcastTx(pendingOutboundTxFFI!!.getId()))
        Thread.sleep(1000)
        verify { mockListener.onOutboundTxBroadcast(any()) }
        val broadcastTx = broadcastTxSlot.captured
        assertEquals(
            TxStatus.BROADCAST,
            broadcastTx.status
        )
        pendingOutboundTxFFI.destroy()

        // mine tx
        for(i in 0 until pendingOutboundTxsFFI.getLength()) {
            pendingOutboundTxFFI = pendingOutboundTxsFFI.getAt(i)
            if (pendingOutboundTxFFI.getStatus() == FFITxStatus.COMPLETED) {
                break
            }
            pendingOutboundTxFFI.destroy()
        }
        pendingOutboundTxsFFI.destroy()
        assertNotNull(pendingOutboundTxFFI)
        val minedTxSlot = slot<CompletedTx>()
        every { mockListener.onTxMined(capture(minedTxSlot)) } answers { }
        assertTrue(wallet.testMineTx(pendingOutboundTxFFI!!.getId()))
        Thread.sleep(1000)
        pendingOutboundTxFFI.destroy()
        verify { mockListener.onTxMined(any()) }
        val minedTx = minedTxSlot.captured
        assertEquals(
            TxStatus.MINED,
            minedTx.status
        )
        // get mined tx by id
        val minedTxFFI = wallet.getCompletedTxById(minedTx.id)
        assertEquals(
            minedTx.id,
            minedTxFFI.getId()
        )
        minedTxFFI.destroy()
    }

    /**
     * No return values from the functions, just testing for no exceptions.
     */
    @Test
    fun testPowerModes() {
        wallet.setPowerModeLow()
        Thread.sleep(2000)
        wallet.setPowerModeNormal()
        Thread.sleep(2000)
    }

    private class TestListener : FFIWalletListener {

        val receivedTxs = mutableListOf<PendingInboundTx>()
        val finalizedTxs = mutableListOf<PendingInboundTx>()
        val minedTxs = mutableListOf<CompletedTx>()
        val replyReceivedTxs = mutableListOf<PendingOutboundTx>()
        val cancelledTxs = mutableListOf<CancelledTx>()
        val inboundBroadcastTxs = mutableListOf<PendingInboundTx>()
        val outboundBroadcastTxs = mutableListOf<PendingOutboundTx>()

        override fun onBaseNodeSyncComplete(requestId: BigInteger, success: Boolean) {
            Logger.i(
                "Base Node Sync Complete :: request id %s success %s",
                requestId.toString(),
                success.toString()
            )
        }

        override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
            Logger.i("Tx Received :: pending inbound tx id %s", pendingInboundTx.id)
            receivedTxs.add(pendingInboundTx)
        }

        override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
            Logger.i(
                "Tx Reply Received :: pending outbound tx id %s",
                pendingOutboundTx.id
            )
            replyReceivedTxs.add(pendingOutboundTx)
        }

        override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
            Logger.i(
                "Tx Finalized :: pending inbound tx id: %s",
                pendingInboundTx.id
            )
            finalizedTxs.add(pendingInboundTx)
        }

        override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
            Logger.i(
                "Inbound tx Broadcast :: pending inbound tx id %s",
                pendingInboundTx.id
            )
            inboundBroadcastTxs.add(pendingInboundTx)
        }

        override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
            Logger.i(
                "Outbound tx Broadcast :: pending outbound tx id %s",
                pendingOutboundTx.id
            )
            outboundBroadcastTxs.add(pendingOutboundTx)
        }

        override fun onTxMined(completedTx: CompletedTx) {
            Logger.i("Tx Mined :: completed tx id: %s", completedTx.id)
            minedTxs.add(completedTx)
        }

        override fun onTxCancelled(cancelledTx: CancelledTx) {
            Logger.i("Tx Cancelled :: cancelled tx id: %s", cancelledTx.id)
            cancelledTxs.add(cancelledTx)
        }

        override fun onDirectSendResult(txId: BigInteger, success: Boolean) {
            Logger.i("Direct send :: tx id %s success %s", txId, success)
        }

        override fun onStoreAndForwardSendResult(txId: BigInteger, success: Boolean) {
            Logger.i(
                "Store and forward :: tx id %s success %s",
                txId,
                success
            )
        }

    }

}
