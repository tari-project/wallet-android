package com.tari.android.wallet.ui.fragment.store.webView

import android.animation.ValueAnimator
import android.view.View

class NavigationPanelAnimation(private val view: View) {
    private var state = NavigationPanelAnimationState(TranslationDirection.UP, null)

    fun processScroll(dy: Int) {
        // Ignore weak scrolls
        if (dy >= -5 && dy <= 5) return
        if (dy > 0 && this.state.direction != TranslationDirection.DOWN) {
            state = createState(TranslationDirection.DOWN, to = view.height.toFloat())
        } else if (dy < 0 && this.state.direction != TranslationDirection.UP) {
            state = createState(TranslationDirection.UP, to = 0F)
        }
    }

    private fun createState(direction: TranslationDirection, to: Float) =
        NavigationPanelAnimationState(direction, ValueAnimator.ofFloat(view.translationY, to).apply {
            duration = TRANSLATION_DURATION
            addUpdateListener { view.translationY = it.animatedValue as Float }
            start()
        })

    fun dispose() {
        this.state.animator?.cancel()
    }

    private companion object {
        private const val TRANSLATION_DURATION = 300L
    }
}