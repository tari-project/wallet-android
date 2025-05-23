package com.tari.android.wallet.ui.screen.send.finalize

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.animation.addListener
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.databinding.ViewFinalizingStepBinding
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.removeListenersAndCancel
import com.tari.android.wallet.util.extension.visible
import com.tari.android.wallet.util.Constants

class FinalizingStepView : FrameLayout {

    private var progressAnimationToggleCount = 0
    var progressAnim: ValueAnimator? = null

    lateinit var ui: ViewFinalizingStepBinding
    lateinit var step: FinalizeSendTxViewModel.FinalizingStep
    lateinit var ticAction: () -> Unit

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        ui = ViewFinalizingStepBinding.inflate(LayoutInflater.from(context), this, false)
        addView(ui.root)

        ui.stepProgressBarContainerView.alpha = 0f
        ui.stepProgressBar.apply {
            invisible()
            progress = 0
            max = FinalizeSendTxFragment.MAX_PROGRESS
        }
    }

    fun setup(step: FinalizeSendTxViewModel.FinalizingStep, ticAction: () -> Unit) {
        this.step = step
        this.ticAction = ticAction
    }

    fun animateCurrentStepProgress(isReverse: Boolean) {
        // On config change the UI is already cleared out but this method can be invoked
        ui.stepProgressBar.visible()
        val minValue = if (isReverse) FinalizeSendTxFragment.MAX_PROGRESS else 0
        val maxValue = if (isReverse) 0 else FinalizeSendTxFragment.MAX_PROGRESS
        progressAnim = ValueAnimator.ofInt(minValue, maxValue).apply {
            addUpdateListener { valueAnimator: ValueAnimator -> ui.stepProgressBar.progress = valueAnimator.animatedValue as Int }
            duration = FinalizeSendTxFragment.PROGRESS_BAR_FILL_DURATION_MS
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.xShortDurationMs
            addListener(onEnd = {
                progressAnimationToggleCount++
                removeAllListeners()
                if (progressAnimationToggleCount >= 3 && !isReverse) {
                    tryToProceedToTheNextStepOnProgressAnimCompletion(!isReverse)
                } else {
                    animateCurrentStepProgress(!isReverse)
                }
            })
            start()
        }
    }

    private fun tryToProceedToTheNextStepOnProgressAnimCompletion(isReverse: Boolean) {
        ticAction()
        if (!step.isCompleted) {
            animateCurrentStepProgress(isReverse)
        } else {
            progressAnim?.removeAllListeners()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnim?.removeListenersAndCancel()
    }
}