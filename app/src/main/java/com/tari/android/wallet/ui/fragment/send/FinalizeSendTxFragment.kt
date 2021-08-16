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
package com.tari.android.wallet.ui.fragment.send

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentFinalizeSendTxBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Seconds
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Displays the successful outgoing transaction animation.
 *
 * @author The Tari Development Team
 */
class FinalizeSendTxFragment : Fragment(), ServiceConnection {

    enum class FailureReason {
        NETWORK_CONNECTION_ERROR,
        BASE_NODE_CONNECTION_ERROR,
        SEND_ERROR;
    }

    private enum class Step {
        CONNECTION_CHECK,
        DISCOVERY,
        SENT;

        /**
         * The 2-line text that gets displayed during the step.
         */
        lateinit var descLine1: String
        lateinit var descLine2: String
    }

    @Inject
    lateinit var tracker: Tracker

    /**
     * Tx properties.
     */
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari
    private lateinit var note: String

    private lateinit var listenerWR: WeakReference<Listener>
    private val lottieAnimationPauseProgress = 0.3f

    private var sentTxId: TxId? = null

    private var currentStep = Step.CONNECTION_CHECK
    private val maxProgress = 100

    // progress from 0-to-max and otherwise both increment this field
    private var progressAnimationToggleCount = 0
    private val progressBarFillDurationMs = 850L
    private var switchToNextProgressStateOnProgressAnimComplete = false
    private var progressAnim: ValueAnimator? = null
    private val connectionTimeoutSecs = 30
    private lateinit var connectionCheckStartTime: DateTime

    /**
     * Will be set when the send is successful.
     */
    private var failureReason: FailureReason? = null

    /**
     * Both send methods have to fail to arrive at the judgement that the send has failed.
     */
    private var directSendHasFailed = false
    private var storeAndForwardHasFailed = false

    private lateinit var walletService: TariWalletService
    private lateinit var ui: FragmentFinalizeSendTxBinding

    // region Fragment lifecycle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    override fun onStart() {
        super.onStart()
        val mActivity = activity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ui.backgroundAnimationVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }
        ui.backgroundAnimationVideoView.setVideoURI(mActivity.getResourceUri(R.raw.sending_background))
        ui.backgroundAnimationVideoView.setOnPreparedListener { mp -> mp.isLooping = true }
        ui.backgroundAnimationVideoView.start()
    }

    override fun onStop() {
        ui.backgroundAnimationVideoView.stopPlayback()
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentFinalizeSendTxBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        bindToWalletService()
        listenerWR.get()?.onSendTxStarted(this)
        // get tx properties
        recipientUser = requireArguments().getParcelable("recipientUser")!!
        amount = requireArguments().getParcelable("amount")!!
        note = requireArguments().getString("note")!!
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/finalize", title = "Send Tari - Finalize")
        }
    }

    private fun bindToWalletService() {
        val context = requireActivity()
        val bindIntent = Intent(context, WalletService::class.java)
        context.bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("FinalizeSendTxFragment onServiceConnected")
        walletService = TariWalletService.Stub.asInterface(service)
        setupUi()
        // start checking network connection
        connectionCheckStartTime = DateTime.now()
        checkConnectionStatus()
        subscribeToEventBus()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("FinalizeSendTxFragment onServiceConnected")
        // No-op
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.torProxyState.unsubscribe(this)
        EventBus.unsubscribe(this)
        ui.lottieAnimationView.removeAllAnimatorListeners()
        requireActivity().unbindService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.unsubscribe(this)
    }

    // endregion

    private fun setupUi() {
        ui.lottieAnimationView.setMaxProgress(lottieAnimationPauseProgress)

        Step.CONNECTION_CHECK.descLine1 = string(finalize_send_tx_sending_step_1_desc_line_1)
        Step.CONNECTION_CHECK.descLine2 = string(finalize_send_tx_sending_step_1_desc_line_2)
        Step.DISCOVERY.descLine1 = string(finalize_send_tx_sending_step_2_desc_line_1)
        Step.DISCOVERY.descLine2 = string(finalize_send_tx_sending_step_2_desc_line_2)
        Step.SENT.descLine1 = string(finalize_send_tx_sending_step_3_desc_line_1)
        Step.SENT.descLine2 = string(finalize_send_tx_sending_step_3_desc_line_2)

        ui.infoLine1TextView.invisible()
        ui.infoLine2TextView.invisible()
        currentStep = Step.CONNECTION_CHECK
        ui.step1ProgressBarContainerView.alpha = 0f
        ui.step1ProgressBar.visible()
        ui.step1ProgressBar.progress = 0
        ui.step1ProgressBar.max = maxProgress
        ui.step2ProgressBarContainerView.alpha = 0f
        ui.step2ProgressBar.invisible()
        ui.step2ProgressBar.progress = 0
        ui.step2ProgressBar.max = maxProgress
        ui.step3ProgressBarContainerView.alpha = 0f
        ui.step3ProgressBar.invisible()
        ui.step2ProgressBar.progress = 0
        ui.step2ProgressBar.max = maxProgress
        ui.rootView.postDelayed({
            fadeInProgressBarContainers()
            ui.lottieAnimationView.playAnimation()
            playCurrentStepTextAppearAnimation()
        }, Constants.UI.FinalizeSendTx.lottieAnimStartDelayMs)
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Transaction.DirectSendResult>(this) { event ->
            ui.rootView.post { onDirectSendResult(event.txId, event.success) }
        }
        EventBus.subscribe<Event.Transaction.StoreAndForwardSendResult>(this) { event ->
            ui.rootView.post { onStoreAndForwardSendResult(event.txId, event.success) }
        }
    }

    private fun playCurrentStepTextAppearAnimation() {
        // line 1
        ui.infoLine1TextView.text = currentStep.descLine1
        ui.infoLine1TextView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        ui.infoLine1TextView.invisible()
        ui.infoLine1TextView.translationY = ui.infoLine1TextView.height.toFloat()
        ui.infoLine1TextView.alpha = 1f
        ui.infoLine1TextView.visible()
        ObjectAnimator.ofFloat(
            ui.infoLine1TextView,
            "translationY",
            ui.infoLine1TextView.height.toFloat(),
            0f
        ).apply {
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs
            start()
        }

        // line 2
        ui.infoLine2TextView.text = currentStep.descLine2
        ui.infoLine2TextView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        ui.infoLine2TextView.invisible()
        ui.infoLine2TextView.translationY = ui.infoLine2TextView.height.toFloat()
        ui.infoLine2TextView.alpha = 1f
        ui.infoLine2TextView.visible()
        ObjectAnimator.ofFloat(
            ui.infoLine2TextView,
            "translationY",
            ui.infoLine2TextView.height.toFloat(),
            0F
        ).apply {
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay =
                Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs + Constants.UI.xShortDurationMs
            start()
        }
    }

    private fun fadeInProgressBarContainers() {
        val fadeAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.step1ProgressBarContainerView.alpha = alpha
            ui.step2ProgressBarContainerView.alpha = alpha
            ui.step3ProgressBarContainerView.alpha = alpha
        }
        fadeAnim.duration = Constants.UI.longDurationMs
        fadeAnim.startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs
        fadeAnim.addListener(onEnd = { animateCurrentStepProgress(isReverse = false) })
        fadeAnim.start()
    }

    private fun animateCurrentStepProgress(isReverse: Boolean) {
        // On config change the UI is already cleared out but this method can be invoked
        val progressBar = when (currentStep) {
            Step.CONNECTION_CHECK -> ui.step1ProgressBar
            Step.DISCOVERY -> ui.step2ProgressBar
            else -> ui.step3ProgressBar
        }
        progressBar.visible()
        progressAnim =
            (if (isReverse) ValueAnimator.ofInt(maxProgress, 0)
            else ValueAnimator.ofInt(0, maxProgress))
                .also { animator ->
                    animator.addUpdateListener { valueAnimator: ValueAnimator ->
                        progressBar.progress = valueAnimator.animatedValue as Int
                    }
                    animator.duration = progressBarFillDurationMs
                    animator.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                    animator.startDelay = Constants.UI.xShortDurationMs
                    animator.addListener(onEnd = {
                        progressAnimationToggleCount++
                        animator.removeAllListeners()
                        if (progressAnimationToggleCount >= 3 && !isReverse) {
                            tryToProceedToTheNextStepOnProgressAnimCompletion()
                        } else {
                            animateCurrentStepProgress(!isReverse)
                        }
                    })
                    animator.start()
                }
    }

    private fun tryToProceedToTheNextStepOnProgressAnimCompletion() {
        failureReason?.let {
            onFailure(it)
            return@tryToProceedToTheNextStepOnProgressAnimCompletion
        }
        if (switchToNextProgressStateOnProgressAnimComplete) {
            switchToNextProgressStateOnProgressAnimComplete = false
            goToNextStep()
            return
        } else if (currentStep == Step.SENT) {
            onSuccess()
            return
        }
        if (currentStep == Step.CONNECTION_CHECK) {
            val secondsElapsed = Seconds.secondsBetween(
                connectionCheckStartTime,
                DateTime.now()
            ).seconds
            if (secondsElapsed >= connectionTimeoutSecs) { // network connection timeout
                val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
                if (networkConnectionState != NetworkConnectionState.CONNECTED) {
                    // internet connection problem
                    onFailure(FailureReason.NETWORK_CONNECTION_ERROR)
                } else {
                    // tor connection problem
                    onFailure(FailureReason.BASE_NODE_CONNECTION_ERROR)
                }
                return
            }
        }
        animateCurrentStepProgress(isReverse = true)
    }

    /**
     * Step #1 of the flow.
     */
    private fun checkConnectionStatus() {
        val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
        val torProxyState = EventBus.torProxyState.publishSubject.value
        // check internet connection
        if (networkConnectionState != NetworkConnectionState.CONNECTED) {
            Logger.w("Send error: not connected to the internet.")
            // either not connected or Tor proxy is not running
            failureReason = FailureReason.NETWORK_CONNECTION_ERROR
            return
        }
        // check whether Tor proxy is running
        if (torProxyState !is TorProxyState.Running) {
            Logger.w("Send error: Tor proxy is not running.")
            // either not connected or Tor proxy is not running
            failureReason = FailureReason.NETWORK_CONNECTION_ERROR
            return
        }
        // check Tor bootstrap status
        if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.maxProgress) {
            Logger.d("Tor proxy not ready - start waiting.")
            // subscribe to Tor proxy state changes - start waiting on it
            EventBus.torProxyState.subscribe(this) { state ->
                onTorProxyStateChanged(state)
            }
        } else {
            switchToNextProgressStateOnProgressAnimComplete = true
        }
    }

    @SuppressLint("CheckResult")
    private fun onTorProxyStateChanged(torProxyState: TorProxyState) {
        if (torProxyState is TorProxyState.Running) {
            if (torProxyState.bootstrapStatus.progress == TorBootstrapStatus.maxProgress) {
                EventBus.torProxyState.unsubscribe(this)
                checkConnectionStatus()
            }
        }
    }

    private fun fadeOutTextViews(completion: () -> Unit) {
        val alphaAnim = ValueAnimator.ofFloat(1F, 0F)
        alphaAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.infoLine1TextView.alpha = alpha
            ui.infoLine2TextView.alpha = alpha
        }
        alphaAnim.duration = Constants.UI.mediumDurationMs
        alphaAnim.addListener(onEnd = {
            alphaAnim.removeAllListeners()
            completion()
        })
        alphaAnim.start()
    }

    private fun goToNextStep() {
        when (currentStep) {
            Step.CONNECTION_CHECK -> {
                fadeOutTextViews {
                    progressAnimationToggleCount = 0
                    currentStep = Step.DISCOVERY
                    playCurrentStepTextAppearAnimation()
                    animateCurrentStepProgress(isReverse = false)
                    sendTari()
                }
            }
            Step.DISCOVERY -> {
                fadeOutTextViews {
                    progressAnimationToggleCount = 0
                    currentStep = Step.SENT
                    playCurrentStepTextAppearAnimation()
                    animateCurrentStepProgress(isReverse = false)
                }
            }
            Step.SENT -> {
                onSuccess()
            }
        }
    }

    private fun sendTari() {
        listenerWR.get()?.onSendTxStarted(this)
        val error = WalletError()
        lifecycleScope.launch(Dispatchers.IO) {
            val txId = walletService.sendTari(
                recipientUser,
                amount,
                Constants.Wallet.defaultFeePerGram,
                note,
                error
            )
            // if success, just wait for the callback to happen
            // if failed, just show the failed info & return
            if (txId == null || error.code != WalletErrorCode.NO_ERROR) {
                failureReason = FailureReason.SEND_ERROR
            } else {
                sentTxId = txId
            }
        }
    }

    private fun onDirectSendResult(txId: TxId, success: Boolean) {
        if (sentTxId != txId) {
            Logger.d("Response received for another tx with id: ${txId.value}.")
            return
        }
        Logger.d("Direct Send completed with result: $success.")
        if (success) {
            // track event
            tracker.event(category = "Transaction", action = "Transaction Accepted - Synchronous")
            // progress state
            switchToNextProgressStateOnProgressAnimComplete = true
            EventBus.unsubscribe(this)
        } else {
            directSendHasFailed = true
            checkForCombinedFailure()
        }
    }

    private fun onStoreAndForwardSendResult(txId: TxId, success: Boolean) {
        if (sentTxId != txId) {
            Logger.d("Response received for another tx with id: ${txId.value}.")
            return
        }
        Logger.d("Store and forward send completed with result: $success.")
        if (success) {
            // track event
            tracker.event(category = "Transaction", action = "Transaction Stored")
            // progress state
            switchToNextProgressStateOnProgressAnimComplete = true
            EventBus.unsubscribe(this)
        } else {
            storeAndForwardHasFailed = true
            checkForCombinedFailure()
        }
    }

    private fun checkForCombinedFailure() {
        if (directSendHasFailed && storeAndForwardHasFailed) { // both have failed
            failureReason = FailureReason.SEND_ERROR
        }
    }

    private fun onFailure(failureReason: FailureReason) {
        progressAnim?.removeAllListeners()
        ui.lottieAnimationView.speed = -1f
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress

        // track event
        val trackerEvent = when (failureReason) {
            FailureReason.NETWORK_CONNECTION_ERROR -> "Transaction Failed - Tor Issue"
            FailureReason.BASE_NODE_CONNECTION_ERROR -> "Transaction Failed - Node Issue"
            FailureReason.SEND_ERROR -> "Transaction Failed - Node Issue"
        }
        tracker.event(category = "Transaction", action = trackerEvent)

        // fade out text and progress
        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.infoLine1TextView.alpha = alpha
            ui.infoLine2TextView.alpha = alpha
            ui.step1ProgressBarContainerView.alpha = alpha
            ui.step2ProgressBarContainerView.alpha = alpha
            ui.step3ProgressBarContainerView.alpha = alpha
        }
        fadeOutAnim.duration = Constants.UI.xLongDurationMs
        fadeOutAnim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        fadeOutAnim.addListener(onEnd = {
            fadeOutAnim.removeAllListeners()
            ui.lottieAnimationView.alpha = 0f
            listenerWR.get()?.onSendTxFailure(
                this, recipientUser,
                amount,
                note,
                failureReason
            )

        })
        fadeOutAnim.start()
    }

    private fun onSuccess() {
        ui.lottieAnimationView.setMaxProgress(1.0f)
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress
        // fade out text and progress
        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.infoLine1TextView.alpha = alpha
            ui.infoLine2TextView.alpha = alpha
            ui.step1ProgressBarContainerView.alpha = alpha
            ui.step2ProgressBarContainerView.alpha = alpha
            ui.step3ProgressBarContainerView.alpha = alpha
        }
        fadeOutAnim.duration = Constants.UI.longDurationMs
        fadeOutAnim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        fadeOutAnim.startDelay = Constants.UI.FinalizeSendTx.successfulInfoFadeOutAnimStartDelayMs
        fadeOutAnim.addListener(onEnd = {
            fadeOutAnim.removeAllListeners()
            ui.lottieAnimationView.alpha = 0f
            listenerWR.get()?.onSendTxSuccessful(
                this,
                sentTxId!!,
                recipientUser,
                amount,
                note
            )
        })
        fadeOutAnim.start()
    }

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        fun onSendTxStarted(sourceFragment: FinalizeSendTxFragment)

        /**
         * Recipient is user.
         */
        fun onSendTxFailure(
            sourceFragment: FinalizeSendTxFragment,
            recipientUser: User,
            amount: MicroTari,
            note: String,
            failureReason: FailureReason
        )

        /**
         * Recipient is user.
         */
        fun onSendTxSuccessful(
            sourceFragment: FinalizeSendTxFragment,
            txId: TxId,
            recipientUser: User,
            amount: MicroTari,
            note: String
        )

    }

}
