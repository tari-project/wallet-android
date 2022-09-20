package com.tari.android.wallet.ui.fragment.store.webView

import android.animation.Animator

data class NavigationPanelAnimationState(
    val direction: TranslationDirection, val animator: Animator?
)