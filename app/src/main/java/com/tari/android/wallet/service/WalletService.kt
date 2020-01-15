/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
import android.os.*
import androidx.core.app.NotificationCompat
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.activity.home.HomeActivity
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
    }

    @Inject
    internal lateinit var wallet: FFITestWallet
    @Inject
    @Named(WalletModule.NAME_WALLET_FILES_DIR_PATH)
    internal lateinit var datastorePath: String

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
        listeners.iterator().forEach {
            it.onTxBroadcast(TxId(completedTxId))
        }
    }

    override fun onTxMined(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId mined.")
        listeners.iterator().forEach {
            it.onTxMined(TxId(completedTxId))
        }
    }

    override fun onTxReceived(pendingInboundTxId: BigInteger) {
        Logger.d("Tx $pendingInboundTxId received.")
        listeners.iterator().forEach {
            it.onTxReceived(TxId(pendingInboundTxId))
        }
    }

    override fun onTxReplyReceived(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId reply received.")
        listeners.iterator().forEach {
            it.onTxReplyReceived(TxId(completedTxId))
        }
    }

    override fun onTxFinalized(completedTxId: BigInteger) {
        Logger.d("Tx $completedTxId finalized.")
        listeners.iterator().forEach {
            it.onTxFinalized(TxId(completedTxId))
        }
    }

    override fun onDiscoveryComplete(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId discovery completed. Success: $success")
        listeners.iterator().forEach {
            it.onDiscoveryComplete(TxId(txId), success)
        }
    }

    /**
     * Implementation of the AIDL service definition.
     */
    inner class TariWalletServiceImpl : TariWalletService.Stub() {

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

        override fun generateTestData(): Boolean {
            return wallet.generateTestData(datastorePath)
        }

        override fun getPublicKeyHexString() = wallet.getPublicKey().toString()

        override fun getBalanceInfo() = BalanceInfo(
            wallet.getAvailableBalance(),
            wallet.getPendingIncomingBalance(),
            wallet.getPendingOutgoingBalance()
        )

        override fun getContacts(): List<Contact>? {
            val contactsFFI = wallet.getContacts()
            val contacts = mutableListOf<Contact>()
            for (i in 0 until contactsFFI.getLength()) {
                val contactFFI = contactsFFI.getAt(i)
                val publicKeyFFI = contactFFI.getPublicKey()
                contacts.add(
                    Contact(publicKeyFFI.toString(), contactFFI.getAlias())
                )
                // destroy native objects
                publicKeyFFI.destroy()
                contactFFI.destroy()
            }
            // destroy native collection
            contactsFFI.destroy()
            return contacts
        }

        override fun getCompletedTxs(): List<CompletedTx>? {
            val completedTxsFFI = wallet.getCompletedTxs()
            val completedTxs = mutableListOf<CompletedTx>()
            for (i in 0 until completedTxsFFI.getLength()) {
                val completedTxFFI = completedTxsFFI.getAt(i)
                val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
                val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
                val status = when(completedTxFFI.getStatus()) {
                    FFICompletedTx.Status.TX_NULL_ERROR -> CompletedTx.Status.TX_NULL_ERROR
                    FFICompletedTx.Status.BROADCAST -> CompletedTx.Status.BROADCAST
                    FFICompletedTx.Status.COMPLETED -> CompletedTx.Status.COMPLETED
                    FFICompletedTx.Status.MINED -> CompletedTx.Status.MINED
                }
                completedTxs.add(
                    CompletedTx(
                        completedTxFFI.getId(),
                        sourcePublicKeyFFI.toString(),
                        destinationPublicKeyFFI.toString(),
                        completedTxFFI.getAmount(),
                        completedTxFFI.getFee(),
                        completedTxFFI.getTimestamp(),
                        completedTxFFI.getMessage(),
                        status
                    )
                )
                // destroy native objects
                sourcePublicKeyFFI.destroy()
                destinationPublicKeyFFI.destroy()
                completedTxFFI.destroy()
            }
            // destroy native collection
            completedTxsFFI.destroy()
            return completedTxs
        }

        override fun getCompletedTxById(id: TxId): CompletedTx? {
            val completedTxFFI = wallet.getCompletedTxById(id.value.toLong())
            val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
            val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
            val status = when(completedTxFFI.getStatus()) {
                FFICompletedTx.Status.TX_NULL_ERROR -> CompletedTx.Status.TX_NULL_ERROR
                FFICompletedTx.Status.BROADCAST -> CompletedTx.Status.BROADCAST
                FFICompletedTx.Status.COMPLETED -> CompletedTx.Status.COMPLETED
                FFICompletedTx.Status.MINED -> CompletedTx.Status.MINED
            }
            val completedTx = CompletedTx(
                completedTxFFI.getId(),
                sourcePublicKeyFFI.toString(),
                destinationPublicKeyFFI.toString(),
                completedTxFFI.getAmount(),
                completedTxFFI.getFee(),
                completedTxFFI.getTimestamp(),
                completedTxFFI.getMessage(),
                status
            )
            sourcePublicKeyFFI.destroy()
            destinationPublicKeyFFI.destroy()
            completedTxFFI.destroy()
            return completedTx
        }

        override fun getPendingInboundTxs(): List<PendingInboundTx>? {
            val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
            val pendingInboundTxs = mutableListOf<PendingInboundTx>()
            for (i in 0 until pendingInboundTxsFFI.getLength()) {
                val pendingInboundTxFFI = pendingInboundTxsFFI.getAt(i)
                val sourcePublicKeyFFI = pendingInboundTxFFI.getSourcePublicKey()
                pendingInboundTxs.add(
                    PendingInboundTx(
                        pendingInboundTxFFI.getId(),
                        sourcePublicKeyFFI.toString(),
                        pendingInboundTxFFI.getAmount(),
                        pendingInboundTxFFI.getTimestamp(),
                        pendingInboundTxFFI.getMessage()
                    )
                )
                // destroy native objects
                sourcePublicKeyFFI.destroy()
                pendingInboundTxFFI.destroy()
            }
            // destroy native collection
            pendingInboundTxsFFI.destroy()
            return pendingInboundTxs
        }

        override fun getPendingInboundTxById(id: TxId): PendingInboundTx? {
            val pendingInboundTxFFI = wallet.getPendingInboundTxById(id.value.toLong())
            val sourcePublicKeyFFI = pendingInboundTxFFI.getSourcePublicKey()
            val pendingInboundTx = PendingInboundTx(
                pendingInboundTxFFI.getId(),
                sourcePublicKeyFFI.toString(),
                pendingInboundTxFFI.getAmount(),
                pendingInboundTxFFI.getTimestamp(),
                pendingInboundTxFFI.getMessage()
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            pendingInboundTxFFI.destroy()
            return pendingInboundTx
        }

        override fun getPendingOutboundTxs(): List<PendingOutboundTx>? {
            val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
            val pendingOutboundTxs = mutableListOf<PendingOutboundTx>()
            for (i in 0 until pendingOutboundTxsFFI.getLength()) {
                val pendingOutboundTxFFI = pendingOutboundTxsFFI.getAt(i)
                val destinationPublicKeyFFI = pendingOutboundTxFFI.getDestinationPublicKey()
                pendingOutboundTxs.add(
                    PendingOutboundTx(
                        pendingOutboundTxFFI.getId(),
                        destinationPublicKeyFFI.toString(),
                        pendingOutboundTxFFI.getAmount(),
                        pendingOutboundTxFFI.getTimestamp(),
                        pendingOutboundTxFFI.getMessage()
                    )
                )
                // destroy native objects
                destinationPublicKeyFFI.destroy()
                pendingOutboundTxFFI.destroy()
            }
            // destroy native collection
            pendingOutboundTxsFFI.destroy()
            return pendingOutboundTxs
        }

        override fun getPendingOutboundTxById(id: TxId): PendingOutboundTx? {
            val pendingOutboundTxFFI = wallet.getPendingOutboundTxById(id.value.toLong())
            val destinationPublicKeyFFI = pendingOutboundTxFFI.getDestinationPublicKey()
            val pendingOutboundTx = PendingOutboundTx(
                pendingOutboundTxFFI.getId(),
                destinationPublicKeyFFI.toString(),
                pendingOutboundTxFFI.getAmount(),
                pendingOutboundTxFFI.getTimestamp(),
                pendingOutboundTxFFI.getMessage()
            )
            // destroy native objects
            destinationPublicKeyFFI.destroy()
            pendingOutboundTxFFI.destroy()
            return pendingOutboundTx
        }

        override fun send(
            destinationPublicKeyHexString: String,
            amount: Amount,
            fee: Amount,
            message: String
        ): Boolean {
            val destinationPublicKey = FFIPublicKey(HexString(destinationPublicKeyHexString))
            return wallet.testSendTx(
                destinationPublicKey,
                amount.value.toLong(),
                fee.value.toLong(),
                message
            )
        }

    }

}