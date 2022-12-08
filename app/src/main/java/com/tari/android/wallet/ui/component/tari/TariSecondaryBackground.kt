package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute

class TariSecondaryBackground(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_background_secondary)
        setBackgroundColor(backColor)
    }
}