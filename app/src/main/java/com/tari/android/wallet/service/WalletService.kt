/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
package com.tari.android.wallet.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.activity.home.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import javax.inject.Inject
import com.tari.android.wallet.model.Tx.Direction.*

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
class WalletService : Service(), FFIWalletListenerAdapter {

    companion object {
        // notification id
        private const val NOTIFICATION_ID = 1
        private const val MESSAGE_PREFIX = "Hello Tari from"
    }

    @Inject
    internal lateinit var app: TariWalletApplication
    @Inject
    internal lateinit var wallet: FFITestWallet
    @Inject
    internal lateinit var tariRESTService: TariRESTService
    @Inject
    internal lateinit var notificationHelper: NotificationHelper

    /**
     * Service stub implementation.
     */
    private val serviceImpl = TariWalletServiceImpl()

    /**
     * Registered listeners.
     */
    private var listeners = mutableListOf<TariWalletServiceListener>()

    override fun onCreate() {
        super.onCreate()
        (application as TariWalletApplication).appComponent.inject(this)
        wallet.listenerAdapter = this
    }

    /**
     * Called on service start-up.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = notificationHelper.buildForegroundServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
        Logger.d("Tari wallet service started.")
        return START_NOT_STICKY
    }

    /**
     * Bound service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        Logger.d("Service bound.")
        return serviceImpl
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.d("Service unbound.")
        return super.onUnbind(intent)
    }

    /**
     * A broadcast is made on destroy to get the service running again.
     */
    override fun onDestroy() {
        Logger.d("Service destroyed.")
        sendBroadcast(
            Intent(this, ServiceRestartBroadcastReceiver::class.java)
        )
        super.onDestroy()
    }

    override fun onTxBroadcast(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId broadcast.")
        val txId = TxId(completedTxId)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxBroadcast(txId))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxBroadcast(txId)
        }
    }

    override fun onTxMined(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId mined.")
        val txId = TxId(completedTxId)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxMined(txId))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxMined(txId)
        }
    }

    override fun onTxReceived(pendingInboundTxId: BigInteger) {
        Logger.d("Tx $pendingInboundTxId received.")
        // post event to bus for the internal listeners
        val tx = serviceImpl.getPendingInboundTxById(
            TxId(pendingInboundTxId),
            WalletError()
        )
        if (tx != null) {
            EventBus.post(Event.Wallet.TxReceived(tx))
            // manage notifications
            postTxNotification(tx)
        }
        // notify listeners
        val txId = TxId(pendingInboundTxId)
        listeners.iterator().forEach {
            it.onTxReceived(txId)
        }
    }

    override fun onTxReplyReceived(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId reply received.")
        val txId = TxId(completedTxId)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxReplyReceived(txId))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxReplyReceived(txId)
        }
    }

    override fun onTxFinalized(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId finalized.")
        val txId = TxId(completedTxId)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxFinalized(txId))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxFinalized(txId)
        }
    }

    override fun onDiscoveryComplete(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId discovery completed. Success: $success")
        // post event to bus
        EventBus.post(Event.Wallet.DiscoveryComplete(TxId(txId), success))
        // notify external listeners
        listeners.iterator().forEach {
            it.onDiscoveryComplete(TxId(txId), success)
        }
    }

    private fun postTxNotification(tx: Tx) {
        // if app is backgrounded, display heads-up notification
        if (!app.isInForeground || app.currentActivity !is HomeActivity) {
            notificationHelper.postCustomLayoutTxNotification(tx)
        }
    }

    /**
     * Implementation of the AIDL service definition.
     */
    inner class TariWalletServiceImpl : TariWalletService.Stub() {

        /**
         * Maps the throwable into the error out parameter.
         */
        private fun mapThrowableIntoError(throwable: Throwable, error: WalletError) {
            error.code = WalletErrorCode.UNKNOWN_ERROR
            error.message = throwable.message
            if (throwable is FFIException) {
                if (throwable.error != null) {
                    error.code = WalletErrorCode.fromCode(throwable.error.code)
                }
            }
        }

        private fun getContactByPublicKeyHexString(
            allContacts: List<Contact>,
            hexString: String
        ): Contact? {
            allContacts.iterator().forEach {
                if (it.publicKey.hexString == hexString) {
                    return it
                }
            }
            return null
        }

        override fun registerListener(listener: TariWalletServiceListener): Boolean {
            listeners.add(listener)
            listener.asBinder().linkToDeath({
                val removeSuccessful = listeners.remove(listener)
                Logger.i("Listener died. Remove successful: $removeSuccessful.")
            }, 0)
            return true
        }

        override fun unregisterListener(listener: TariWalletServiceListener): Boolean {
            return listeners.remove(listener)
        }

        override fun getPublicKeyHexString(error: WalletError): String? {
            return try {
                wallet.getPublicKey().toString()
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Wallet balance info.
         */
        override fun getBalanceInfo(error: WalletError): BalanceInfo? {
            return try {
                BalanceInfo(
                    MicroTari(wallet.getAvailableBalance()),
                    MicroTari(wallet.getPendingIncomingBalance()),
                    MicroTari(wallet.getPendingOutgoingBalance())
                )
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Get all contacts.
         */
        override fun getContacts(error: WalletError): List<Contact>? {
            try {
                val contactsFFI = wallet.getContacts()
                val contacts = mutableListOf<Contact>()
                for (i in 0 until contactsFFI.getLength()) {
                    val contactFFI = contactsFFI.getAt(i)
                    val publicKeyFFI = contactFFI.getPublicKey()
                    contacts.add(
                        Contact(
                            publicKeyFromFFI(publicKeyFFI),
                            contactFFI.getAlias()
                        )
                    )
                    // destroy native objects
                    publicKeyFFI.destroy()
                    contactFFI.destroy()
                }
                // destroy native collection
                contactsFFI.destroy()
                return contacts.sortedWith(compareBy { it.alias })
            } catch (throwable: Throwable) {
                error.code = WalletErrorCode.UNKNOWN_ERROR
                error.message = throwable.message
                if (throwable is FFIException) {
                    if (throwable.error != null) {
                        error.code = WalletErrorCode.fromCode(throwable.error.code)
                    }
                }
                return null
            }
        }

        /**
         * Gets all users that this wallet had a transaction with, and returns a list
         * of most recent ones, limited by the limit parameter.
         */
        override fun getRecentTxUsers(maxCount: Int, error: WalletError): MutableList<User>? {
            // pre-fetch contacs
            val allContacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || allContacts == null) {
                return null
            }
            val txs = ArrayList<Tx>()
            // collect all transactions
            txs.addAll(getPendingInboundTxs(allContacts))
            txs.addAll(getPendingOutboundTxs(allContacts))
            txs.addAll(getCompletedTxs(allContacts))
            // sort them by descending timestamp
            val sortedTxs = txs.sortedWith(compareByDescending { it.timestamp })
            val recentTxUsers = mutableListOf<User>()

            for (tx in sortedTxs) {
                if (recentTxUsers.size >= maxCount) { // comes first for the case of (maxCount <= 0)
                    break
                }
                if (!recentTxUsers.contains(tx.user)) {
                    val txUser = getContactByPublicKeyHexString(
                        allContacts,
                        tx.user.publicKey.hexString
                    ) ?: tx.user
                    recentTxUsers.add(txUser)
                }
            }
            return recentTxUsers
        }

        /**
         * Internal function to get all completed transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getCompletedTxs(allContacts: List<Contact>): List<CompletedTx> {
            val completedTxsFFI = wallet.getCompletedTxs()
            val completedTxs = mutableListOf<CompletedTx>()
            for (i in 0 until completedTxsFFI.getLength()) {
                val completedTxFFI = completedTxsFFI.getAt(i)
                val completedTx = completedTxFromFFI(completedTxFFI, allContacts)
                completedTxFFI.destroy()
                completedTxs.add(completedTx)
            }
            // destroy native collection
            completedTxsFFI.destroy()
            return completedTxs
        }


        /**
         * Get all completed transactions.
         * Client-facing function.
         */
        override fun getCompletedTxs(error: WalletError): List<CompletedTx>? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getCompletedTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a completed transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getCompletedTxById(id: TxId, allContacts: List<Contact>): CompletedTx {
            val completedTxFFI = wallet.getCompletedTxById(id.value)
            val completedTx = completedTxFromFFI(completedTxFFI, allContacts)
            completedTxFFI.destroy()
            return completedTx
        }

        /**
         * Get completed transaction by id.
         * Client-facing function.
         */
        override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getCompletedTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get all pending inbound transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingInboundTxs(allContacts: List<Contact>): List<PendingInboundTx> {
            val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
            val pendingInboundTxs = mutableListOf<PendingInboundTx>()
            for (i in 0 until pendingInboundTxsFFI.getLength()) {
                val pendingInboundTxFFI = pendingInboundTxsFFI.getAt(i)
                val pendingInboundTx = pendingInboundTxFromFFI(pendingInboundTxFFI, allContacts)
                pendingInboundTxFFI.destroy()
                pendingInboundTxs.add(pendingInboundTx)
            }
            // destroy native collection
            pendingInboundTxsFFI.destroy()
            return pendingInboundTxs
        }

        /**
         * Get all pending inbound transactions.
         * Client-facing function.
         */
        override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingInboundTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a pending inbound transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingInboundTxById(
            id: TxId,
            allContacts: List<Contact>
        ): PendingInboundTx {
            val pendingInboundTxFFI = wallet.getPendingInboundTxById(id.value)
            val pendingInboundTx = pendingInboundTxFromFFI(pendingInboundTxFFI, allContacts)
            pendingInboundTxFFI.destroy()
            return pendingInboundTx
        }

        /**
         * Get pending inbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingInboundTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get all pending outbound transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingOutboundTxs(allContacts: List<Contact>): List<PendingOutboundTx> {
            val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
            val pendingOutboundTxs = mutableListOf<PendingOutboundTx>()
            for (i in 0 until pendingOutboundTxsFFI.getLength()) {
                val pendingOutboundTxFFI = pendingOutboundTxsFFI.getAt(i)
                val pendingOutboundTx = pendingOutboundTxFromFFI(pendingOutboundTxFFI, allContacts)
                pendingOutboundTxFFI.destroy()
                pendingOutboundTxs.add(pendingOutboundTx)
            }
            // destroy native collection
            pendingOutboundTxsFFI.destroy()
            return pendingOutboundTxs
        }

        /**
         * Get all pending outbound transactions.
         * Client-facing function.
         */
        override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingOutboundTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a pending outbound transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingOutboundTxById(
            id: TxId,
            allContacts: List<Contact>
        ): PendingOutboundTx {
            val pendingOutboundTxFFI = wallet.getPendingOutboundTxById(id.value)
            val pendingOutboundTx = pendingOutboundTxFromFFI(pendingOutboundTxFFI, allContacts)
            pendingOutboundTxFFI.destroy()
            return pendingOutboundTx
        }

        /**
         * Get pending outbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingOutboundTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        override fun sendTari(
            user: User,
            amount: MicroTari,
            fee: MicroTari,
            message: String,
            error: WalletError
        ): Boolean {
            return try {
                val publicKeyFFI = FFIPublicKey(HexString(user.publicKey.hexString))
                val success = wallet.sendTx(
                    publicKeyFFI,
                    amount.value,
                    fee.value,
                    message
                )
                publicKeyFFI.destroy()
                success
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                false
            }
        }

        // region FFI to model extraction functions
        private fun publicKeyFromFFI(
            publicKeyFFI: FFIPublicKey
        ): PublicKey {
            return PublicKey(
                publicKeyFFI.toString(),
                publicKeyFFI.getEmojiNodeId()
            )
        }

        private fun completedTxFromFFI(
            completedTxFFI: FFICompletedTx,
            allContacts: List<Contact>
        ): CompletedTx {
            val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
            val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
            val status = when (completedTxFFI.getStatus()) {
                FFIStatus.TX_NULL_ERROR -> Status.TX_NULL_ERROR
                FFIStatus.BROADCAST -> Status.BROADCAST
                FFIStatus.COMPLETED -> Status.COMPLETED
                FFIStatus.MINED -> Status.MINED
                FFIStatus.IMPORTED -> Status.IMPORTED
                FFIStatus.PENDING -> Status.PENDING
                FFIStatus.UNKNOWN -> Status.UNKNOWN
            }
            val user: User
            val direction: Tx.Direction

            // get public key
            val error = WalletError()
            val publicKeyHexString = getPublicKeyHexString(error)
            if (error.code != WalletErrorCode.NO_ERROR) {
                throw FFIException(message = error.message)
            }

            if (publicKeyHexString == destinationPublicKeyFFI.toString()) {
                direction = INBOUND
                val userPublicKey = PublicKey(
                    sourcePublicKeyFFI.toString(),
                    sourcePublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    sourcePublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            } else {
                direction = OUTBOUND
                val userPublicKey = PublicKey(
                    destinationPublicKeyFFI.toString(),
                    destinationPublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    destinationPublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            }
            val completedTx = CompletedTx(
                completedTxFFI.getId(),
                direction,
                user,
                MicroTari(completedTxFFI.getAmount()),
                completedTxFFI.getFee(),
                completedTxFFI.getTimestamp(),
                completedTxFFI.getMessage(),
                status
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            destinationPublicKeyFFI.destroy()
            return completedTx
        }

        private fun pendingInboundTxFromFFI(
            pendingInboundTxFFI: FFIPendingInboundTx,
            allContacts: List<Contact>
        ): PendingInboundTx {
            val status = when (pendingInboundTxFFI.getStatus()) {
                FFIStatus.TX_NULL_ERROR -> Status.TX_NULL_ERROR
                FFIStatus.BROADCAST -> Status.BROADCAST
                FFIStatus.COMPLETED -> Status.COMPLETED
                FFIStatus.MINED -> Status.MINED
                FFIStatus.IMPORTED -> Status.IMPORTED
                FFIStatus.PENDING -> Status.PENDING
                FFIStatus.UNKNOWN -> Status.UNKNOWN
            }
            val sourcePublicKeyFFI = pendingInboundTxFFI.getSourcePublicKey()
            val userPublicKey = PublicKey(
                sourcePublicKeyFFI.toString(),
                sourcePublicKeyFFI.getEmojiNodeId()
            )
            val user = getContactByPublicKeyHexString(
                allContacts,
                sourcePublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingInboundTx = PendingInboundTx(
                pendingInboundTxFFI.getId(),
                user,
                MicroTari(pendingInboundTxFFI.getAmount()),
                pendingInboundTxFFI.getTimestamp(),
                pendingInboundTxFFI.getMessage(),
                status
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            return pendingInboundTx
        }

        private fun pendingOutboundTxFromFFI(
            pendingOutboundTxFFI: FFIPendingOutboundTx,
            allContacts: List<Contact>
        ): PendingOutboundTx {
            val status = when (pendingOutboundTxFFI.getStatus()) {
                FFIStatus.TX_NULL_ERROR -> Status.TX_NULL_ERROR
                FFIStatus.BROADCAST -> Status.BROADCAST
                FFIStatus.COMPLETED -> Status.COMPLETED
                FFIStatus.MINED -> Status.MINED
                FFIStatus.IMPORTED -> Status.IMPORTED
                FFIStatus.PENDING -> Status.PENDING
                FFIStatus.UNKNOWN -> Status.UNKNOWN
            }
            val destinationPublicKeyFFI = pendingOutboundTxFFI.getDestinationPublicKey()
            val userPublicKey = PublicKey(
                destinationPublicKeyFFI.toString(),
                destinationPublicKeyFFI.getEmojiNodeId()
            )
            val user = getContactByPublicKeyHexString(
                allContacts,
                destinationPublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingOutboundTx = PendingOutboundTx(
                pendingOutboundTxFFI.getId(),
                user,
                MicroTari(pendingOutboundTxFFI.getAmount()),
                pendingOutboundTxFFI.getTimestamp(),
                pendingOutboundTxFFI.getMessage(),
                status
            )
            // destroy native objects
            destinationPublicKeyFFI.destroy()
            return pendingOutboundTx
        }

        override fun requestTestnetTari(error: WalletError) {
            // get public key
            val publicKeyHexString = getPublicKeyHexString(error)
            if (error.code != WalletErrorCode.NO_ERROR || publicKeyHexString == null) {
                notifyTestnetTariRequestFailed("Service error.")
                return
            }

            val message = "$MESSAGE_PREFIX $publicKeyHexString"
            val signing = wallet.signMessage(message)
            val signature = signing.split("|")[0]
            val nonce = signing.split("|")[1]

            val requestBody = TestnetTariAllocateRequest(signature, nonce)

            val response = tariRESTService.requestTestnetTari(publicKeyHexString, requestBody)
            response.enqueue(object : Callback<TestnetTariAllocateResponse> {
                override fun onFailure(call: Call<TestnetTariAllocateResponse>, t: Throwable) {
                    error.code = WalletErrorCode.UNKNOWN_ERROR
                    notifyTestnetTariRequestFailed(getString(R.string.wallet_service_error_no_internet_connection))
                }

                override fun onResponse(
                    call: Call<TestnetTariAllocateResponse>,
                    response: Response<TestnetTariAllocateResponse>
                ) {
                    val body = response.body()
                    if (response.code() in 200..209 && body != null) {
                        val publicKeyFFI =
                            FFIPublicKey(HexString(body.returnWalletId))
                        val privateKeyFFI = FFIPrivateKey(HexString(body.key))
                        val value = BigInteger(body.value)
                        val contactFFI = FFIContact("TariBot", publicKeyFFI)
                        wallet.addUpdateContact(contactFFI)
                        wallet.importUTXO(
                            value,
                            getString(R.string.home_tari_bot_some_tari_to_get_started),
                            privateKeyFFI,
                            publicKeyFFI
                        )
                        val publicKey = publicKeyFromFFI(publicKeyFFI)
                        // destroy native objects
                        publicKeyFFI.destroy()
                        privateKeyFFI.destroy()
                        contactFFI.destroy()

                        // post event to bus for the internal listeners
                        EventBus.post(Event.Testnet.TestnetTariRequestSuccessful(publicKey))
                        // notify external listeners
                        listeners.iterator().forEach { listener ->
                            listener.onTestnetTariRequestSuccess()
                        }
                        // post notification
                        getCompletedTxs(WalletError())
                            ?.filter { it.direction == INBOUND && it.user.publicKey == publicKey }
                            ?.forEach { tx ->
                                postTxNotification(tx)
                            }
                    } else {
                        error.code = WalletErrorCode.UNKNOWN_ERROR
                        val errorMessage =
                            getString(R.string.wallet_service_error_testnet_tari_request) +
                                    " " +
                                    response.errorBody()?.string()
                        error.message = errorMessage
                        notifyTestnetTariRequestFailed(errorMessage)
                    }
                }
            })
        }

        private fun notifyTestnetTariRequestFailed(error: String) {
            // post event to bus for the internal listeners
            EventBus.post(Event.Testnet.TestnetTariRequestError(error))
            // notify external listeners
            listeners.iterator().forEach { listener ->
                listener.onTestnetTariRequestError(error)
            }
        }

        override fun updateContactAlias(
            publicKey: PublicKey,
            contactName: String,
            error: WalletError
        ) {
            try {
                val publicKeyFFI = FFIPublicKey(HexString(publicKey.hexString))
                val contact = FFIContact(contactName, publicKeyFFI)
                wallet.addUpdateContact(contact)
                publicKeyFFI.destroy()
                contact.destroy()
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
            }
        }

        /**
         * @return public key constructed from input emoji id. Null if the emoji id is invalid
         * or it does not correspond to a public key.
         */
        override fun getPublicKeyFromEmojiId(emojiId: String?): PublicKey? {
            try {
                FFIPublicKey(emojiId ?: "").run {
                    val publicKey = publicKeyFromFFI(this)
                    destroy()
                    return publicKey
                }
            } catch (ignored: Throwable) {
                return null
            }
        }

        /**
         * @return public key constructed from input public key hex string id. Null if the emoji id
         * is invalid or it does not correspond to a public key.
         */
        override fun getPublicKeyFromHexString(publicKeyHex: String?): PublicKey? {
            try {
                FFIPublicKey(HexString(publicKeyHex ?: "")).run {
                    val publicKey = publicKeyFromFFI(this)
                    destroy()
                    return publicKey
                }
            } catch (ignored: Throwable) {
                return null
            }
        }

        // endregion
    }
}