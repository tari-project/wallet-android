package com.tari.android.wallet.ui.component.tari

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.TariBackButtonBinding
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener

class TariBackButton(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    var backPressedAction: () -> Unit = { (context as? Activity)?.onBackPressed() }

    val ui: TariBackButtonBinding = TariBackButtonBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(ui.root)

        ui.backCtaView.setOnThrottledClickListener { backPressedAction.invoke() }
    }
}