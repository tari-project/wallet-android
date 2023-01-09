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
package com.tari.android.wallet.ui.fragment.tx.ui.progressController

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.addListener
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Controls the transaction list update (pull to refresh) views.
 *
 * @author The Tari Development Team
 */
class UpdateProgressViewController(private val view: View, listener: Listener) : CoroutineScope {

    private val mJob = Job()
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    enum class FailureReason {
        NETWORK_CONNECTION_ERROR,
        BASE_NODE_VALIDATION_ERROR
    }

    enum class State {
        IDLE,
        RUNNING,
        RECEIVING
    }

    class UpdateProgressState {
        var state = State.IDLE

        var numberOfReceivedTxs = 0

        var numberOfBroadcastTxs = 0

        var numberOfCancelledTxs = 0
    }


    private val hourglassIconTextView: TextView = view.findViewById(R.id.home_txt_update_hourglass_icon)
    private val handshakeIconTextView: TextView = view.findViewById(R.id.home_txt_update_handshake_icon)
    private val checkingForUpdatesTextView: TextView = view.findViewById(R.id.home_txt_checking_for_updates)
    private val receivingTxsTextView: TextView = view.findViewById(R.id.home_txt_receiving_txs)
    private val completingTxsTextView: TextView = view.findViewById(R.id.home_txt_completing_txs)
    private val updatingTxsTextView: TextView = view.findViewById(R.id.home_txt_updating_txs)
    private val upToDateTextView: TextView = view.findViewById(R.id.home_txt_up_to_date)
    private val progressBar: ProgressBar = view.findViewById(R.id.home_progress_bar_update)

    val state = UpdateProgressState()

    private var isReset = true

    private var baseNodeSyncCurrentRetryCount = 0
    private val baseNodeSyncMaxRetryCount = 3
    private var connectionCheckStartTimeMs = 0L

    // connection status check (internet, tor, base node) timeout period
    private val timeoutPeriodMs = 40 * 1000L
    private val minStateDisplayPeriodMs = 3 * 1000L
    private var baseNodeSyncTimeoutSubscription: Disposable? = null

    private val listenerWeakReference: WeakReference<Listener> = WeakReference(listener)

    private lateinit var walletService: TariWalletService

    private lateinit var currentTextView: TextView

    private var torBootstrapStatusSubscription: Disposable? = null

    init {
        progressBar.invisible()
        subscribeToEventBus()
    }

    fun reset() {
        if (isReset) return
        baseNodeSyncCurrentRetryCount = 0
        // emojis
        hourglassIconTextView.setTopMargin(0)
        hourglassIconTextView.alpha = 1f
        handshakeIconTextView.alpha = 1f
        // text views
        checkingForUpdatesTextView.visible()
        checkingForUpdatesTextView.setTopMargin(0)
        receivingTxsTextView.visible()
        receivingTxsTextView.setTopMargin(0)
        completingTxsTextView.visible()
        completingTxsTextView.setTopMargin(0)
        updatingTxsTextView.visible()
        updatingTxsTextView.setTopMargin(0)
        upToDateTextView.visible()
        upToDateTextView.setTopMargin(0)
        upToDateTextView.alpha = 1f
        isReset = true
    }

    fun start(walletService: TariWalletService) {
        this.walletService = walletService
        progressBar.visible()
        isReset = false
        state.numberOfReceivedTxs = 0
        state.numberOfCancelledTxs = 0
        state.numberOfBroadcastTxs = 0
        state.state = State.RUNNING
        currentTextView = checkingForUpdatesTextView
        connectionCheckStartTimeMs = System.currentTimeMillis()
        checkNetworkConnectionStatus()
    }

    private fun subscribeToEventBus() {
        EventBus.baseNodeSyncState.subscribe(this) { event ->
            if (state.state == State.RUNNING) {
                onBaseNodeSyncCompleted(event)
            }
        }
        EventBus.subscribe<Event.Transaction.TxReceived>(this) {
            if (state.state == State.RUNNING || state.state == State.RECEIVING) {
                state.numberOfReceivedTxs++
            }
        }
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) {
            if (state.state == State.RUNNING || state.state == State.RECEIVING) {
                state.numberOfBroadcastTxs++
            }
        }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) {
            if (state.state == State.RUNNING || state.state == State.RECEIVING) {
                state.numberOfBroadcastTxs++
            }
        }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) {
            if (state.state == State.RUNNING || state.state == State.RECEIVING) {
                state.numberOfCancelledTxs++
            }
        }
    }

    private fun connectionCheckHasTimedOut(): Boolean {
        return System.currentTimeMillis() > (connectionCheckStartTimeMs + timeoutPeriodMs)
    }

    private fun checkNetworkConnectionStatus() {
        val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
        if (networkConnectionState != NetworkConnectionState.CONNECTED) {
            view.postDelayed({ fail(FailureReason.NETWORK_CONNECTION_ERROR) }, minStateDisplayPeriodMs)
            return
        }
        checkTorBootstrapStatus()
    }

    @SuppressLint("CheckResult")
    private fun checkTorBootstrapStatus() {
        torBootstrapStatusSubscription?.dispose()
        // check for expiration
        if (connectionCheckHasTimedOut()) {
            fail(FailureReason.BASE_NODE_VALIDATION_ERROR)
            return
        }
        val torProxyState = EventBus.torProxyState.publishSubject.value
        // check whether Tor proxy is running
        if (torProxyState !is TorProxyState.Running) {
            // either not connected or Tor proxy is not running
            view.postDelayed({ fail(FailureReason.BASE_NODE_VALIDATION_ERROR) }, minStateDisplayPeriodMs)
            return
        }
        // check Tor bootstrap status
        if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.maxProgress) {
            // check again after a wait period
            torBootstrapStatusSubscription = Observable
                .timer(minStateDisplayPeriodMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { checkTorBootstrapStatus() }
            return
        }
        startBaseNodeSync()
    }

    private fun startBaseNodeSync() {
        // check for expiration
        if (connectionCheckHasTimedOut()) {
            fail(FailureReason.BASE_NODE_VALIDATION_ERROR)
            return
        }
        baseNodeSyncCurrentRetryCount++
        // sync base node
        val walletError = WalletError()
        // long running call
        launch(Dispatchers.IO) {
            val success = walletService.startBaseNodeSync(walletError)
            if (isActive && (!success || walletError != WalletError.NoError)) {
                fail(FailureReason.BASE_NODE_VALIDATION_ERROR)
            }
        }
        // setup expiration timer
        val toTimeoutMs = (connectionCheckStartTimeMs + timeoutPeriodMs) - System.currentTimeMillis()
        baseNodeSyncTimeoutSubscription = Observable
            .timer(toTimeoutMs, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { baseNodeSyncTimedOut() }
    }

    private fun onBaseNodeSyncCompleted(event: BaseNodeSyncState) {
        when (event) {
            is BaseNodeSyncState.Online -> {
                baseNodeSyncTimeoutSubscription?.dispose()
                state.state = State.RECEIVING
                view.postDelayed({ displayReceivingTxs() }, minStateDisplayPeriodMs)
            }
            is BaseNodeSyncState.Failed -> {
                baseNodeSyncTimeoutSubscription?.dispose()
                if (baseNodeSyncCurrentRetryCount >= baseNodeSyncMaxRetryCount) {
                    fail(FailureReason.BASE_NODE_VALIDATION_ERROR)
                } else {
                    startBaseNodeSync()
                }
            }
            else -> Unit
        }
    }

    private fun displayReceivingTxs() {
        if (state.numberOfReceivedTxs == 0) {
            receivingTxsTextView.gone()
            displayCompletingTxs()
            return
        }
        animateToTextView(receivingTxsTextView)
        view.postDelayed({ displayCompletingTxs() }, minStateDisplayPeriodMs)
    }

    private fun displayCompletingTxs() {
        if (state.numberOfBroadcastTxs == 0) {
            completingTxsTextView.gone()
            displayUpdatingTxs()
            return
        }
        animateToTextView(completingTxsTextView)
        view.postDelayed({ displayUpdatingTxs() }, minStateDisplayPeriodMs)
    }

    private fun displayUpdatingTxs() {
        if (state.numberOfCancelledTxs == 0) {
            updatingTxsTextView.gone()
            displayUpToDate()
            return
        }
        animateToTextView(updatingTxsTextView)
        view.postDelayed({ displayUpToDate() }, minStateDisplayPeriodMs)
    }

    private fun displayUpToDate() {
        animateToTextView(upToDateTextView)
        progressBar.invisible()
        view.postDelayed({ complete() }, Constants.UI.xLongDurationMs)
    }

    private fun complete() {
        state.state = State.IDLE
        listenerWeakReference.get()?.updateHasCompleted(this, state.numberOfReceivedTxs, state.numberOfCancelledTxs)
    }

    private fun baseNodeSyncTimedOut() {
        fail(FailureReason.BASE_NODE_VALIDATION_ERROR)
    }

    private fun fail(failureReason: FailureReason) {
        state.state = State.IDLE
        listenerWeakReference.get()?.updateHasFailed(this, failureReason)
        view.post { progressBar.invisible() }
    }

    fun destroy() {
        EventBus.unsubscribe(this)
        baseNodeSyncTimeoutSubscription?.dispose()
        torBootstrapStatusSubscription?.dispose()
        cancel()
    }

    private fun animateToTextView(nextTextView: TextView, onComplete: (() -> Unit)? = null) {
        // animate text view
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                currentTextView.setTopMargin((-currentTextView.height * value).toInt())
                if (currentTextView == checkingForUpdatesTextView && nextTextView != upToDateTextView) {
                    hourglassIconTextView.setTopMargin((-hourglassIconTextView.height * value).toInt())
                } else if (nextTextView == upToDateTextView) {
                    hourglassIconTextView.alpha = 1f - value
                    handshakeIconTextView.alpha = 1f - value
                }
            }
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            addListener(onEnd = {
                removeAllListeners()
                currentTextView = nextTextView
                onComplete?.let { callback -> callback() }
            })
            start()
        }
    }

    interface Listener {
        fun updateHasFailed(source: UpdateProgressViewController, failureReason: FailureReason)

        fun updateHasCompleted(source: UpdateProgressViewController, receivedTxCount: Int, cancelledTxCount: Int)
    }
}
