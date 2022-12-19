package com.tari.android.wallet.ui.fragment.utxos.list.controllers

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
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
            view.setTextColor(view.context.colorFromAttribute(R.attr.palette_text_links))
        } else {
            view.setText(R.string.utxos_selecting)
            view.setTextColor(view.context.colorFromAttribute(R.attr.palette_text_heading))
        }
    }
}