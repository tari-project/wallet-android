package com.tari.android.wallet.ui.fragment.onboarding

import android.app.Activity
import android.view.View
import com.tari.android.wallet.ui.activity.restore.WalletRestoreActivity

class WalletRestorationListener(private val parent: Activity) : View.OnClickListener {
    override fun onClick(v: View?) =
        parent.startActivity(WalletRestoreActivity.navigationIntent(parent))
}
