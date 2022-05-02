package com.tari.android.wallet.ui.fragment.send.addNote.gif

import android.animation.ValueAnimator
import android.view.View

class GIFsPanelAnimation(private val view: View) {
    private var state = GIFsPanelAnimationState(TranslationDirection.UP, null)
    val isViewShown
        get() = state.direction == TranslationDirection.UP

    fun show() {
        state.animator?.cancel()
        state = createState(TranslationDirection.UP, to = 0F)
    }

    fun hide() {
        state.animator?.cancel()
        state = createState(TranslationDirection.DOWN, to = view.height.toFloat())
    }

    private fun createState(direction: TranslationDirection, to: Float) =
        GIFsPanelAnimationState(direction, ValueAnimator.ofFloat(view.translationY, to).apply {
            duration = TRANSLATION_DURATION
            addUpdateListener {
                view.translationY = it.animatedValue as Float
            }
            start()
        })

    fun dispose() {
        this.state.animator?.cancel()
    }

    private companion object {
        private const val TRANSLATION_DURATION = 300L
    }
}