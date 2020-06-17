package com.tari.android.wallet.ui.fragment.onboarding

import android.app.Activity
import android.view.View

// Used in different flavors, edit with caution
class WalletRestorationListener(@Suppress("UNUSED_PARAMETER") parent: Activity) :
    View.OnClickListener {
    // No-op for private builds
    override fun onClick(v: View?) = Unit
}
