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
package com.tari.android.wallet.ui.fragment.send.finalize

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentFinalizeSendTxBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.CustomFontTextView
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import org.joda.time.DateTime
import org.joda.time.Seconds
import java.lang.ref.WeakReference

class FinalizeSendTxFragment : CommonFragment<FragmentFinalizeSendTxBinding, FinalizeSendTxViewModel>() {

    private lateinit var finalizeSendTxListenerWR: WeakReference<FinalizeSendTxListener>

    // progress from 0-to-max and otherwise both increment this field
    private var progressAnimationToggleCount = 0
    private var progressAnim: ValueAnimator? = null

    // region Fragment lifecycle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        finalizeSendTxListenerWR = WeakReference(context as FinalizeSendTxListener)
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

        val viewModel: FinalizeSendTxViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.transactionData = requireArguments().getParcelable(FinalizeSendTxViewModel.transactionDataKey)!!
        setupUi()
        subscribeUI()
    }

    private fun subscribeUI() = with(viewModel) {
        observeOnLoad(txFailureReason)
        observeOnLoad(sentTxId)
        observeOnLoad(currentStep)
        observeOnLoad(finishedSending)
        observeOnLoad(torConnected)
    }

    override fun onStart() {
        super.onStart()
        with(ui.backgroundAnimationVideoView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            }
            setVideoURI(requireActivity().getResourceUri(R.raw.sending_background))
            setOnPreparedListener { mp -> mp.isLooping = true }
            start()
        }
    }

    override fun onStop() {
        ui.backgroundAnimationVideoView.stopPlayback()
        super.onStop()
    }

    // endregion

    private fun setupUi() = with(ui) {
        lottieAnimationView.setMaxProgress(lottieAnimationPauseProgress)
        infoLine1TextView.invisible()
        infoLine2TextView.invisible()
        step1ProgressBarContainerView.alpha = 0f
        step1ProgressBar.apply {
            visible()
            progress = 0
            max = maxProgress
        }
        step2ProgressBarContainerView.alpha = 0f
        step2ProgressBar.apply {
            invisible()
            progress = 0
            max = maxProgress
        }
        step3ProgressBarContainerView.alpha = 0f
        step1ProgressBar.apply {
            invisible()
            progress = 0
            max = maxProgress
        }
        rootView.postDelayed({
            fadeInProgressBarContainers()
            ui.lottieAnimationView.playAnimation()
            playCurrentStepTextAppearAnimation()
        }, Constants.UI.FinalizeSendTx.lottieAnimStartDelayMs)
    }

    private fun playCurrentStepTextAppearAnimation() {
        playStepAppearAnimation(viewModel.currentStep.value!!.descLine1, ui.infoLine1TextView)
        playStepAppearAnimation(viewModel.currentStep.value!!.descLine2, ui.infoLine2TextView, Constants.UI.xShortDurationMs)
    }

    private fun playStepAppearAnimation(lineText: String, textView: CustomFontTextView, additionalDelay: Long = 0) = with(textView) {
        text = lineText
        measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        invisible()
        translationY = height.toFloat()
        alpha = 1f
        visible()
        ObjectAnimator.ofFloat(this, "translationY", height.toFloat(), 0f).apply {
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs + additionalDelay
            start()
        }
    }

    private fun fadeInProgressBarContainers() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.step1ProgressBarContainerView.alpha = alpha
                ui.step2ProgressBarContainerView.alpha = alpha
                ui.step3ProgressBarContainerView.alpha = alpha
            }
            duration = Constants.UI.longDurationMs
            startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs
            addListener(onEnd = { animateCurrentStepProgress(isReverse = false) })
            start()
        }
    }

    private fun animateCurrentStepProgress(isReverse: Boolean) {
        // On config change the UI is already cleared out but this method can be invoked
        val progressBar = when (viewModel.currentStep.value!!) {
            is FinalizingStep.ConnectionCheck -> ui.step1ProgressBar
            is FinalizingStep.Discovery -> ui.step2ProgressBar
            else -> ui.step3ProgressBar
        }
        progressBar.visible()
        val minValue = if (isReverse) maxProgress else 0
        val maxValue = if (isReverse) 0 else maxProgress
        progressAnim = ValueAnimator.ofInt(minValue, maxValue).apply {
            addUpdateListener { valueAnimator: ValueAnimator -> progressBar.progress = valueAnimator.animatedValue as Int }
            duration = progressBarFillDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.xShortDurationMs
            addListener(onEnd = {
                progressAnimationToggleCount++
                removeAllListeners()
                if (progressAnimationToggleCount >= 3 && !isReverse) {
                    tryToProceedToTheNextStepOnProgressAnimCompletion()
                } else {
                    animateCurrentStepProgress(!isReverse)
                }
            })
            start()
        }
    }

    private fun tryToProceedToTheNextStepOnProgressAnimCompletion() {
        viewModel.txFailureReason.value?.let {
            onFailure(it)
            return@tryToProceedToTheNextStepOnProgressAnimCompletion
        }

        if (viewModel.switchToNextProgressStateOnProgressAnimComplete) {
            viewModel.switchToNextProgressStateOnProgressAnimComplete = false
            goToNextStep()
            return
        } else if (viewModel.currentStep.value is FinalizingStep.Sent) {
            onSuccess()
            return
        }
        if (viewModel.currentStep.value is FinalizingStep.ConnectionCheck) {
            val secondsElapsed = Seconds.secondsBetween(viewModel.connectionCheckStartTime, DateTime.now()).seconds
            if (secondsElapsed >= connectionTimeoutSecs) { // network connection timeout
                val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
                if (networkConnectionState != NetworkConnectionState.CONNECTED) {
                    // internet connection problem
                    onFailure(TxFailureReason.NETWORK_CONNECTION_ERROR)
                } else {
                    // tor connection problem
                    onFailure(TxFailureReason.BASE_NODE_CONNECTION_ERROR)
                }
                return
            }
        }
        animateCurrentStepProgress(isReverse = true)
    }

    private fun fadeOutTextViews(completion: () -> Unit) {
        ValueAnimator.ofFloat(1F, 0F).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
            }
            duration = Constants.UI.mediumDurationMs
            addListener(onEnd = {
                removeAllListeners()
                completion()
            })
            start()
        }
    }

    private fun goToNextStep() {
        when (viewModel.currentStep.value) {
            is FinalizingStep.ConnectionCheck -> {
                showNextStep(FinalizingStep.Discovery(viewModel.resourceManager))
                viewModel.sendTari()
            }
            is FinalizingStep.Discovery -> showNextStep(FinalizingStep.Sent(viewModel.resourceManager))
            is FinalizingStep.Sent -> onSuccess()
        }
    }

    private fun showNextStep(step: FinalizingStep) {
        fadeOutTextViews {
            progressAnimationToggleCount = 0
            viewModel.currentStep.value = step
            playCurrentStepTextAppearAnimation()
            animateCurrentStepProgress(isReverse = false)
        }
    }

    private fun onFailure(txFailureReason: TxFailureReason) {
        progressAnim?.removeAllListeners()
        ui.lottieAnimationView.speed = -1f
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress

        // track event
        val trackerEvent = when (txFailureReason) {
            TxFailureReason.NETWORK_CONNECTION_ERROR -> "Transaction Failed - Tor Issue"
            TxFailureReason.BASE_NODE_CONNECTION_ERROR -> "Transaction Failed - Node Issue"
            TxFailureReason.SEND_ERROR -> "Transaction Failed - Node Issue"
        }
        viewModel.tracker.event(category = "Transaction", action = trackerEvent)

        // fade out text and progress
        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                ui.step1ProgressBarContainerView.alpha = alpha
                ui.step2ProgressBarContainerView.alpha = alpha
                ui.step3ProgressBarContainerView.alpha = alpha
            }
            duration = Constants.UI.xLongDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            addListener(onEnd = {
                removeAllListeners()
                ui.lottieAnimationView.alpha = 0f
                finalizeSendTxListenerWR.get()?.onSendTxFailure(viewModel.transactionData, txFailureReason)
            })
            start()
        }
    }

    private fun onSuccess() {
        ui.lottieAnimationView.setMaxProgress(1.0f)
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress

        // fade out text and progress
        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                ui.step1ProgressBarContainerView.alpha = alpha
                ui.step2ProgressBarContainerView.alpha = alpha
                ui.step3ProgressBarContainerView.alpha = alpha
            }
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.FinalizeSendTx.successfulInfoFadeOutAnimStartDelayMs
            addListener(onEnd = {
                removeAllListeners()
                ui.lottieAnimationView.alpha = 0f
                finalizeSendTxListenerWR.get()?.onSendTxSuccessful(viewModel.sentTxId.value!!, viewModel.transactionData)
            })
            start()
        }
    }

    companion object {
        private const val maxProgress = 100
        private const val progressBarFillDurationMs = 850L
        private const val connectionTimeoutSecs = 30
        private const val lottieAnimationPauseProgress = 0.3f

        fun create(transactionData: TransactionData): FinalizeSendTxFragment {
            return FinalizeSendTxFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FinalizeSendTxViewModel.transactionDataKey, transactionData)
                }
            }
        }
    }
}