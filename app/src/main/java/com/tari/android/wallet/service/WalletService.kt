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
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.activity.home.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
class WalletService : Service(), FFIWalletListenerAdapter {

    companion object {
        // notification channel id
        const val notifChannelId = "TariWalletServiceNotifChannel"
        const val MESSAGE_PREFIX = "Hello Tari from"
    }

    @Inject
    internal lateinit var wallet: FFITestWallet
    @Inject
    @Named(WalletModule.FieldName.walletLogFilePath)
    internal lateinit var mLogFilePath: String
    @Inject
    internal lateinit var tariRESTService: TariRESTService
    /**
     * Service stub implementation.
     */
    private val serviceImpl = TariWalletServiceImpl()
    /**
     * Registered listeners.
     */
    private var listeners = mutableListOf<TariWalletServiceListener>()

    override fun onCreate() {
        Logger.d("Tari wallet service created.")
        super.onCreate()
        (application as TariWalletApplication).appComponent.inject(this)

        // set wallet listener
        wallet.listenerAdapter = this
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                notifChannelId,
                "Tari Wallet Service Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.setSound(null, null)
            serviceChannel.setShowBadge(false)
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Called on service start-up.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Logger.d("Tari wallet service started.")
        createNotificationChannel()
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        // post foreground service notification
        val notification: Notification = NotificationCompat.Builder(this, notifChannelId)
            .setContentTitle(applicationContext.resources.getString(R.string.service_title))
            .setContentText(applicationContext.resources.getString(R.string.service_description))
            .setSound(null)
            .setSmallIcon(R.drawable.wallet_service_notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
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
        val txId = TxId(pendingInboundTxId)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxReceived(txId))
        // notify listeners
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

    /**
     * Implementation of the AIDL service definition.
     */
    inner class TariWalletServiceImpl : TariWalletService.Stub() {

        private fun getContactFromPublicKeyHexString(
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

        override fun getPublicKeyForEmojiId(emojiId: String?): PublicKey? {
            if (emojiId == null || emojiId.isEmpty()) {
                return null
            }
            return try {
                val publicKeyFFI = FFIPublicKey(emojiId)
                val publicKey = publicKeyFromFFI(publicKeyFFI)
                publicKeyFFI.destroy()
                publicKey
            } catch (ignored: Throwable) {
                null
            }
        }

        override fun getLogFilePath(): String {
            return mLogFilePath
        }

        override fun getPublicKeyHexString() = wallet.getPublicKey().toString()

        /**
         * Wallet balance info.
         */
        override fun getBalanceInfo() = BalanceInfo(
            MicroTari(wallet.getAvailableBalance()),
            MicroTari(wallet.getPendingIncomingBalance()),
            MicroTari(wallet.getPendingOutgoingBalance())
        )

        /**
         * Get all contacts.
         */
        override fun getContacts(): List<Contact> {
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
        }

        /**
         * Gets all users that this wallet had a transaction with, and returns a list
         * of most recent ones, limited by the limit parameter.
         */
        override fun getRecentTxUsers(maxCount: Int): MutableList<User> {
            // pre-fetch contacs
            val allContacts = contacts
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
                    val txUser = getContactFromPublicKeyHexString(
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
        override fun getCompletedTxs(): List<CompletedTx> {
            // call the corresponding function with fresh contacts list
            return getCompletedTxs(contacts)
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
        override fun getCompletedTxById(id: TxId): CompletedTx {
            return getCompletedTxById(id, contacts)
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
        override fun getPendingInboundTxs(): List<PendingInboundTx> {
            // call the corresponding function with fresh contacts list
            return getPendingInboundTxs(contacts)
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
        override fun getPendingInboundTxById(id: TxId): PendingInboundTx {
            // call the corresponding function with fresh contacts list
            return getPendingInboundTxById(id, contacts)
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
        override fun getPendingOutboundTxs(): List<PendingOutboundTx> {
            // call the corresponding function with fresh contacts list
            return getPendingOutboundTxs(contacts)
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
        override fun getPendingOutboundTxById(id: TxId): PendingOutboundTx {
            // call the corresponding function with fresh contacts list
            return getPendingOutboundTxById(id, contacts)
        }

        override fun sendTari(
            user: User,
            amount: MicroTari,
            fee: MicroTari,
            message: String
        ): Boolean {
            val publicKeyFFI = FFIPublicKey(HexString(user.publicKey.hexString))
            val success = wallet.sendTx(
                publicKeyFFI,
                amount.value,
                fee.value,
                message
            )
            publicKeyFFI.destroy()
            return success
        }

        // region FFI to model extraction functions

        private fun publicKeyFromFFI(
            publicKeyFFI: FFIPublicKey
        ): PublicKey {
            return PublicKey(
                publicKeyFFI.toString(),
                publicKeyFFI.getEmoji()
            )
        }

        private fun completedTxFromFFI(
            completedTxFFI: FFICompletedTx,
            allContacts: List<Contact>
        ): CompletedTx {
            val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
            val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
            val status = when (completedTxFFI.getStatus()) {
                FFICompletedTx.Status.TX_NULL_ERROR -> CompletedTx.Status.TX_NULL_ERROR
                FFICompletedTx.Status.BROADCAST -> CompletedTx.Status.BROADCAST
                FFICompletedTx.Status.COMPLETED -> CompletedTx.Status.COMPLETED
                FFICompletedTx.Status.MINED -> CompletedTx.Status.MINED
                FFICompletedTx.Status.UNKNOWN -> CompletedTx.Status.UNKNOWN
            }
            val user: User
            val direction: Tx.Direction
            if (publicKeyHexString == destinationPublicKeyFFI.toString()) {
                direction = Tx.Direction.INBOUND
                val userPublicKey = PublicKey(
                    sourcePublicKeyFFI.toString(),
                    sourcePublicKeyFFI.getEmoji()
                )
                user = getContactFromPublicKeyHexString(
                    allContacts,
                    sourcePublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            } else {
                direction = Tx.Direction.OUTBOUND
                val userPublicKey = PublicKey(
                    destinationPublicKeyFFI.toString(),
                    destinationPublicKeyFFI.getEmoji()
                )
                user = getContactFromPublicKeyHexString(
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
            val sourcePublicKeyFFI = pendingInboundTxFFI.getSourcePublicKey()
            val userPublicKey = PublicKey(
                sourcePublicKeyFFI.toString(),
                sourcePublicKeyFFI.getEmoji()
            )
            val user = getContactFromPublicKeyHexString(
                allContacts,
                sourcePublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingInboundTx = PendingInboundTx(
                pendingInboundTxFFI.getId(),
                user,
                MicroTari(pendingInboundTxFFI.getAmount()),
                pendingInboundTxFFI.getTimestamp(),
                pendingInboundTxFFI.getMessage()
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            return pendingInboundTx
        }

        private fun pendingOutboundTxFromFFI(
            pendingOutboundTxFFI: FFIPendingOutboundTx,
            allContacts: List<Contact>
        ): PendingOutboundTx {
            val destinationPublicKeyFFI = pendingOutboundTxFFI.getDestinationPublicKey()
            val userPublicKey = PublicKey(
                destinationPublicKeyFFI.toString(),
                destinationPublicKeyFFI.getEmoji()
            )
            val user = getContactFromPublicKeyHexString(
                allContacts,
                destinationPublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingOutboundTx = PendingOutboundTx(
                pendingOutboundTxFFI.getId(),
                user,
                MicroTari(pendingOutboundTxFFI.getAmount()),
                pendingOutboundTxFFI.getTimestamp(),
                pendingOutboundTxFFI.getMessage()
            )
            // destroy native objects
            destinationPublicKeyFFI.destroy()
            return pendingOutboundTx
        }

        override fun requestTestnetTari() {
            val message = "$MESSAGE_PREFIX $publicKeyHexString"
            val signature = wallet.signMessage(message)
            val requestBody = TestnetTariAllocateRequest(signature, publicKeyHexString)

            val response = tariRESTService.requestTestnetTari(publicKeyHexString, requestBody)
            response.enqueue(object : Callback<TestnetTariAllocateResponse> {
                override fun onFailure(call: Call<TestnetTariAllocateResponse>, t: Throwable) {
                    notifyTestnetTariRequestFailed(getString(R.string.service_error_no_internet_connection))
                }

                override fun onResponse(
                    call: Call<TestnetTariAllocateResponse>,
                    response: Response<TestnetTariAllocateResponse>
                ) {
                    when (response.code()) {
                        in 200..209 -> {
                            response.body()?.let { responseBody ->
                                val publicKeyFFI =
                                    FFIPublicKey(HexString(responseBody.returnWalletId))
                                val privateKeyFFI = FFIPrivateKey(HexString(responseBody.key))
                                val value = BigInteger(responseBody.value)
                                val contactFFI = FFIContact("TariBot", publicKeyFFI)
                                wallet.addUpdateContact(contactFFI)
                                wallet.importUTXO(
                                    value,
                                    getString(R.string.home_tari_bot_some_tari_to_get_started),
                                    privateKeyFFI,
                                    publicKeyFFI
                                )
                                // destroy native objects
                                publicKeyFFI.destroy()
                                privateKeyFFI.destroy()
                                contactFFI.destroy()
                            }
                            // post event to bus for the internal listeners
                            EventBus.post(Event.Testnet.TestnetTariRequestSuccessful())
                            // notify external listeners
                            listeners.iterator().forEach { listener ->
                                listener.onTestnetTariRequestSuccess()
                            }
                        }
                        else -> {
                            val errorMessage =
                                getString(R.string.service_error_testnet_tari_request) +
                                        " " +
                                        response.errorBody()?.string()
                            notifyTestnetTariRequestFailed(errorMessage)
                        }
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
        // endregion
    }
}