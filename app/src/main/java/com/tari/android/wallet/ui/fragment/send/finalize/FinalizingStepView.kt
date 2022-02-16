package com.tari.android.wallet.ui.fragment.send.finalize

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.animation.addListener
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.databinding.ViewFinalizingStepBinding
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.util.Constants

class FinalizingStepView : FrameLayout {

    var progressAnimationToggleCount = 0
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

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        ui = ViewFinalizingStepBinding.inflate(LayoutInflater.from(context), this, false)
        addView(ui.root)

        ui.stepProgressBarContainerView.alpha = 0f
        ui.stepProgressBar.apply {
            invisible()
            progress = 0
            max = FinalizeSendTxFragment.maxProgress
        }
    }

    fun setup(step: FinalizeSendTxViewModel.FinalizingStep, ticAction: () -> Unit) {
        this.step = step
        this.ticAction = ticAction
    }

    fun animateCurrentStepProgress(isReverse: Boolean) {
        // On config change the UI is already cleared out but this method can be invoked
        ui.stepProgressBar.visible()
        val minValue = if (isReverse) FinalizeSendTxFragment.maxProgress else 0
        val maxValue = if (isReverse) 0 else FinalizeSendTxFragment.maxProgress
        progressAnim = ValueAnimator.ofInt(minValue, maxValue).apply {
            addUpdateListener { valueAnimator: ValueAnimator -> ui.stepProgressBar.progress = valueAnimator.animatedValue as Int }
            duration = FinalizeSendTxFragment.progressBarFillDurationMs
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
}