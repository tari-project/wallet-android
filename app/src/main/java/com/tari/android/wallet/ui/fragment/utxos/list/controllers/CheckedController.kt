package com.tari.android.wallet.ui.fragment.utxos.list.controllers

import android.view.View
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import java.util.concurrent.atomic.AtomicBoolean

class CheckedController(val view: View) {

    private var checked: AtomicBoolean = AtomicBoolean(false)

    var toggleCallback: (Boolean) -> Unit = {}

    fun toggleChecked() {
        setChecked(!checked.get())
    }

    fun setChecked(checked: Boolean) {
        this.checked.set(checked)
        toggleCallback(checked)
        val back = if (this.checked.get()) ContextCompat.getDrawable(view.context, R.drawable.background_checked) else null
        view.background = back
    }
}