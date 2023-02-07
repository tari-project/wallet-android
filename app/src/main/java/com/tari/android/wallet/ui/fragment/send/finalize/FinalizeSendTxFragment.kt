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
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentFinalizeSendTxBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.tari.TariTextView
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants

class FinalizeSendTxFragment : CommonFragment<FragmentFinalizeSendTxBinding, FinalizeSendTxViewModel>() {

    private var stepListView = mutableListOf<FinalizingStepView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentFinalizeSendTxBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: FinalizeSendTxViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.transactionData = requireArguments().parcelable(FinalizeSendTxViewModel.transactionDataKey)!!
        viewModel.start()
        setupUi()
        subscribeUI()
    }

    private fun subscribeUI() = with(viewModel) {
        observe(txFailureReason) { onFailure(it) }

        observe(steps) { createAllSteps(it) }

        observe(isSuccess) { onSuccess() }

        observe(nextStep) { showNextStep(it) }

        observeOnLoad(sentTxId)
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

    private fun createAllSteps(steps: List<FinalizeSendTxViewModel.FinalizingStep>) {
        for (step in steps) {
            val view = FinalizingStepView(requireContext())
            view.setup(step) { viewModel.checkStepStatus() }
            stepListView.add(view)
            ui.stepsContainer.addView(view)
        }
    }

    private fun getCurrentStepView(step: FinalizeSendTxViewModel.FinalizingStep): FinalizingStepView? = stepListView.firstOrNull { it.step == step }

    private fun showNextStep(step: FinalizeSendTxViewModel.FinalizingStep) {
        fadeOutTextViews {
            playCurrentStepTextAppearAnimation(step)
            getCurrentStepView(step)?.animateCurrentStepProgress(isReverse = false)
        }
    }

    private fun setupUi() = with(ui) {
        lottieAnimationView.setMaxProgress(lottieAnimationPauseProgress)
        infoLine1TextView.invisible()
        infoLine2TextView.invisible()

        rootView.postDelayed({
            fadeInProgressBarContainers()
            ui.lottieAnimationView.playAnimation()
        }, Constants.UI.FinalizeSendTx.lottieAnimStartDelayMs)
    }

    private fun playCurrentStepTextAppearAnimation(step: FinalizeSendTxViewModel.FinalizingStep) {
        playStepAppearAnimation(step.descriptionLine1, ui.infoLine1TextView)
        playStepAppearAnimation(step.descriptionLine2, ui.infoLine2TextView, Constants.UI.xShortDurationMs)
    }

    private fun playStepAppearAnimation(lineText: String, textView: TariTextView, additionalDelay: Long = 0) = with(textView) {
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
                stepListView.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.longDurationMs
            startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs
            start()
        }
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

    private fun onFailure(txFailureReason: TxFailureReason) {
        stepListView.forEach { it.progressAnim?.removeAllUpdateListeners() }
        ui.lottieAnimationView.speed = -1f
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress

        // fade out text and progress
        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                stepListView.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.xLongDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            addListener(onEnd = {
                runCatching {
                    removeAllListeners()
                    ui.lottieAnimationView.alpha = 0f
                    (requireActivity() as? FinalizeSendTxListener)?.onSendTxFailure(false, viewModel.transactionData, txFailureReason)
                }
            })
            start()
        }
    }

    private fun onSuccess() {
        stepListView.forEach { it.progressAnim?.removeAllUpdateListeners() }
        ui.lottieAnimationView.setMaxProgress(1.0f)
        ui.lottieAnimationView.playAnimation()
        ui.lottieAnimationView.progress = lottieAnimationPauseProgress

        // fade out text and progress
        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                stepListView.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.FinalizeSendTx.successfulInfoFadeOutAnimStartDelayMs
            addListener(onEnd = {
                runCatching {
                    removeAllListeners()
                    ui.lottieAnimationView.alpha = 0f
                    (requireActivity() as? FinalizeSendTxListener)?.onSendTxSuccessful(false, viewModel.sentTxId.value!!, viewModel.transactionData)
                }
            })
            start()
        }
    }

    companion object {
        const val maxProgress = 100
        const val progressBarFillDurationMs = 850L
        const val lottieAnimationPauseProgress = 0.3f

        fun create(transactionData: TransactionData): FinalizeSendTxFragment {
            return FinalizeSendTxFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FinalizeSendTxViewModel.transactionDataKey, transactionData)
                }
            }
        }
    }
}

