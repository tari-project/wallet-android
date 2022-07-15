package com.tari.android.wallet.ui.fragment.utxos.list.controllers

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import java.util.concurrent.atomic.AtomicBoolean

class CheckedController(val view: TextView) {

    private var checked: AtomicBoolean = AtomicBoolean(false)

    var toggleCallback: (Boolean) -> Unit = {}

    fun toggleChecked() {
        setChecked(!checked.get())
    }

    fun setChecked(checked: Boolean) {
        if(checked == this.checked.get()) return
        this.checked.set(checked)
        toggleCallback(checked)
        if (this.checked.get()) {
            view.setText(R.string.common_cancel)
            view.setTextColor(ContextCompat.getColor(view.context, R.color.purple))
        } else {
            view.setText(R.string.utxos_selecting)
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black))
        }
    }
}