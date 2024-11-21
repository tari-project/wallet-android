package com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.ViewConfirmWordBinding
import com.tari.android.wallet.util.extension.setVisible

class SelectableWordTextView : FrameLayout {

    constructor(context: Context) : super(context, null) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(context: Context, isSelected: Boolean) : super(context, null) {
        init(isSelected)
    }

    lateinit var ui: ViewConfirmWordBinding

    private fun init(isSelected: Boolean = false) {
        ui = ViewConfirmWordBinding.inflate(LayoutInflater.from(context), this, false).apply {
            text.background = null
            text.setPadding(0, 0, 0, 0)
        }
        ui.removeView.setVisible(isSelected)
        addView(ui.root)
    }
}