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
package com.tari.android.wallet.ui.screen.send.obsolete.finalize

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.databinding.FragmentFinalizeSendTxBinding
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.component.tari.TariTextView
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.visible

class FinalizeSendTxFragment : CommonXmlFragment<FragmentFinalizeSendTxBinding, FinalizeSendTxViewModel>() {

    private lateinit var stepViewList: List<FinalizingStepView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentFinalizeSendTxBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: FinalizeSendTxViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()
        subscribeUI()

        doOnBackPressed { viewModel.showCancelDialog() }
    }

    private fun subscribeUI() {
        collectFlow(viewModel.uiState) { uiState ->
            createAllSteps(uiState.steps)
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is FinalizeSendTxModel.Effect.SendTxSuccess -> onSuccess()
                is FinalizeSendTxModel.Effect.ShowError -> onFailure()
                is FinalizeSendTxModel.Effect.ShowNextStep -> showNextStep(effect.step)
            }
        }
    }

    private fun createAllSteps(steps: List<FinalizeSendTxViewModel.FinalizingStep>) {
        if (this::stepViewList.isInitialized) return
        stepViewList = steps.map { step ->
            FinalizingStepView(requireContext())
                .also {
                    it.setup(step) { viewModel.checkStepStatus() }
                    ui.stepsContainer.addView(it)
                }
        }
    }

    private fun getCurrentStepView(step: FinalizeSendTxViewModel.FinalizingStep): FinalizingStepView? = stepViewList.firstOrNull { it.step == step }

    private fun showNextStep(step: FinalizeSendTxViewModel.FinalizingStep) {
        fadeOutTextViews {
            playCurrentStepTextAppearAnimation(step)
            getCurrentStepView(step)?.animateCurrentStepProgress(isReverse = false)
        }
    }

    private fun setupUi() = with(ui) {
        infoLine1TextView.invisible()
        infoLine2TextView.invisible()

        rootView.postDelayed({
            fadeInProgressBarContainers()
        }, Constants.UI.FinalizeSendTx.lottieAnimStartDelayMs)
    }

    private fun playCurrentStepTextAppearAnimation(step: FinalizeSendTxViewModel.FinalizingStep) {
        playStepAppearAnimation(
            lineText = string(step.descLine1Res),
            textView = ui.infoLine1TextView,
        )
        playStepAppearAnimation(
            lineText = string(step.descLine2Res),
            textView = ui.infoLine2TextView,
            additionalDelay = Constants.UI.xShortDurationMs,
        )
    }

    private fun playStepAppearAnimation(lineText: String, textView: TariTextView, additionalDelay: Long = 0) = with(textView) {
        text = lineText
        measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        invisible()
        translationY = height.toFloat()
        alpha = 1f
        visible()

        animations += ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, height.toFloat(), 0f).apply {
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
                stepViewList.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.longDurationMs
            startDelay = Constants.UI.FinalizeSendTx.textAppearAnimStartDelayMs
            start()
        }
    }

    private fun fadeOutTextViews(completion: () -> Unit) {
        animations += ValueAnimator.ofFloat(1F, 0F).apply {
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

    private fun onFailure() {
        stepViewList.forEach { it.progressAnim?.removeAllUpdateListeners() }

        // fade out text and progress
        animations += ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                stepViewList.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.xLongDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            addListener(onEnd = {
                runCatching {
                    removeAllListeners()

                    viewModel.trySendTxFailure()
                }
            })
            start()
        }
    }

    private fun onSuccess() {
        stepViewList.forEach { it.progressAnim?.removeAllUpdateListeners() }

        // fade out text and progress
        animations += ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.infoLine1TextView.alpha = alpha
                ui.infoLine2TextView.alpha = alpha
                stepViewList.forEach { it.ui.stepProgressBarContainerView.alpha = alpha }
            }
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.FinalizeSendTx.successfulInfoFadeOutAnimStartDelayMs
            addListener(onEnd = {
                runCatching {
                    removeAllListeners()

                    viewModel.trySendTxSuccess()
                }
            })
            start()
        }
    }

    companion object {
        const val MAX_PROGRESS = 100
        const val PROGRESS_BAR_FILL_DURATION_MS = 850L

        fun create(transactionData: TransactionData): FinalizeSendTxFragment {
            return FinalizeSendTxFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FinalizeSendTxViewModel.KEY_TRANSACTION_DATA, transactionData)
                }
            }
        }
    }
}
