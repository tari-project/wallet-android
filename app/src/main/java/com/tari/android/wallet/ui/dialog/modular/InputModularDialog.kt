package com.tari.android.wallet.ui.dialog.modular

import android.app.Activity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.component.tari.background.obsolete.TariPrimaryBackgroundConstraint


class InputModularDialog(context: Activity) : ModularDialog(context) {

    constructor(context: Activity, args: ModularDialogArgs) : this(context) {
        applyArgs(args)
        modifyDialog()
    }

    private fun modifyDialog() {
        dialog.findViewById<TariPrimaryBackgroundConstraint>(R.id.root)?.apply {
            elevation = 0F
            updateBack(0F, 0F)
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(0, 0, 0, 0)
            }
        }
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)
    }
}