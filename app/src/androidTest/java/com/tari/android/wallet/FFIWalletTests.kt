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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepositoryImpl
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.SecurityStagesPrefRepository
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.di.ApplicationModule
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.data.sharedPrefs.contacts.ContactPrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class FFIWalletTests {

    private lateinit var wallet: FFIWallet
    private lateinit var listener: TestAddRecipientAddNodeListener
    private val context = getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences(ApplicationModule.sharedPrefsFileName, Context.MODE_PRIVATE)
    private val networkRepository = NetworkPrefRepositoryImpl(prefs)
    private val baseNodeSharedPrefsRepository = BaseNodePrefRepository(prefs, networkRepository)
    private val backupSettingsRepository = BackupPrefRepository(context, prefs, networkRepository)
    private val yatSharedPrefsRepository = YatPrefRepository(prefs, networkRepository)
    private val tariSettingsRepository = TariSettingsPrefRepository(prefs, networkRepository)
    private val securityStagesRepository = SecurityStagesPrefRepository(prefs, networkRepository)
    private val contactSharedPrefRepository = ContactPrefRepository(networkRepository, prefs)
    private val sentryPrefRepository: SentryPrefRepository = SentryPrefRepository(prefs, networkRepository)
    private val torSharedRepository = TorPrefRepository(prefs, networkRepository)
    private val securityPrefRepository = SecurityPrefRepository(context, prefs, networkRepository)
    private val sharedPrefsRepository = CorePrefRepository(
        prefs,
        networkRepository,
        backupSettingsRepository,
        baseNodeSharedPrefsRepository,
        yatSharedPrefsRepository,
        torSharedRepository,
        tariSettingsRepository,
        securityStagesRepository,
        contactSharedPrefRepository,
        sentryPrefRepository,
        securityPrefRepository,
    )

    private val walletDirPath = context.filesDir.absolutePath

    private fun clean() {
        val clean = FFITestUtil.clearTestFiles(walletDirPath)
        sharedPrefsRepository.clear()
        if (!clean) {
            throw RuntimeException("Test files could not cleared.")
        }
    }

    @Before
    fun setup() {
        // clean any existing wallet data
        clean()
        // create memory transport
        val transport = FFITariTransportConfig()
        // create comms config
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            FFITestUtil.WALLET_DB_NAME,
            walletDirPath,
            Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
            Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC,
        )
        val logFile = File(walletDirPath, "test_log.log")
        // create wallet instance
        wallet = FFIWallet(sharedPrefsRepository, securityPrefRepository, SeedPhraseRepository(), networkRepository, commsConfig, logFile.absolutePath)
        // create listener
        listener = TestAddRecipientAddNodeListener()
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
        val ffiTariWalletAddress = wallet.getWalletAddress()
        assertNotEquals(nullptr, ffiTariWalletAddress.pointer)
        assertEquals(FFITestUtil.WALLET_ADDRESS_HEX_STRING.length, ffiTariWalletAddress.toString().length)
    }

//    @Test
//    fun signedMessageVerificationWasSuccessful() {
//        val message = "Hello"
//        val signature = wallet.signMessage(message)
//        assertTrue(wallet.verifyMessageSignature(wallet.getPublicKey(), message, signature))
//    }

    @Test
    fun testContacts() {
        val contactCount = 127
        // add contacts
        repeat(contactCount) {
            val contactPrivateKey = FFIPrivateKey.generate()
            val contactWalletAddress = FFITariWalletAddress(contactPrivateKey)
            val contact = FFIContact(
                FFITestUtil.generateRandomAlphanumericString(16),
                contactWalletAddress
            )
            wallet.addUpdateContact(contact)
            contactPrivateKey.destroy()
            contactWalletAddress.destroy()
            contact.destroy()
        }
        // test get contacts
        var contacts = wallet.getContacts()
        assertEquals(contactCount, contacts.getLength())
        // test update alias
        val lastContactOld = contacts.getAt(contactCount - 1)
        val lastContactOldWalletAddress = lastContactOld.getWalletAddress()
        val newAlias = FFITestUtil.generateRandomAlphanumericString(7)
        val lastContactNew = FFIContact(
            newAlias,
            lastContactOldWalletAddress
        )
        lastContactOldWalletAddress.destroy()
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
        assertTrue(emoji.hex().isNotEmpty())
        emoji.destroy()
        emojiSet.destroy()
        assertEquals(nullptr, emojiSet.pointer)
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

    @Test
    fun testKeyValueStorage() {
        val key = "test_emoji_sequence"
        val value = "⛵️🚿😻♐️♊️⌛️"
        assertTrue(wallet.setKeyValue(key, value))
        assertEquals(value, wallet.getKeyValue(key))
        assertTrue(wallet.removeKeyValue(key))
    }

    @Test(expected = FFIException::class)
    fun testKeyValueStorageBadAccess() {
        val key = "test_key"
        val value = "test_value"
        assertTrue(wallet.setKeyValue(key, value))
        assertTrue(wallet.removeKeyValue(key))
        wallet.getKeyValue(key)
    }

    private class TestAddRecipientAddNodeListener : FFIWalletListener {

        val receivedTxs = mutableListOf<PendingInboundTx>()
        val finalizedTxs = mutableListOf<PendingInboundTx>()
        val minedTxs = mutableListOf<CompletedTx>()
        val replyReceivedTxs = mutableListOf<PendingOutboundTx>()
        val cancelledTxs = mutableListOf<CancelledTx>()
        val inboundBroadcastTxs = mutableListOf<PendingInboundTx>()
        val outboundBroadcastTxs = mutableListOf<PendingOutboundTx>()

        override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
            receivedTxs.add(pendingInboundTx)
        }

        override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
            replyReceivedTxs.add(pendingOutboundTx)
        }

        override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
            finalizedTxs.add(pendingInboundTx)
        }

        override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
            inboundBroadcastTxs.add(pendingInboundTx)
        }

        override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
            outboundBroadcastTxs.add(pendingOutboundTx)
        }

        override fun onTxMined(completedTx: CompletedTx) {
            minedTxs.add(completedTx)
        }

        override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
            minedTxs.add(completedTx)
        }

        override fun onTxFauxConfirmed(completedTx: CompletedTx) {
            minedTxs.add(completedTx)
        }

        override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
            minedTxs.add(completedTx)
        }

        override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) {
            cancelledTxs.add(cancelledTx)
        }

        override fun onTXOValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = Unit

        override fun onWalletRestoration(result: WalletRestorationResult) = Unit

        override fun onDirectSendResult(txId: BigInteger, status: TransactionSendStatus) = Unit

        override fun onConnectivityStatus(status: Int) = Unit

        override fun onBalanceUpdated(balanceInfo: BalanceInfo) = Unit

        override fun onTxValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = Unit
    }
}
