package com.tari.android.wallet.ui.component.tari.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.ViewTariToolbarActionBinding
import com.tari.android.wallet.util.extension.setEndMargin
import com.tari.android.wallet.util.extension.setOnThrottledClickListener
import com.tari.android.wallet.util.extension.setStartMargin
import com.tari.android.wallet.util.extension.visible

class TariToolbarActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyle, defStyleRes) {

    private val ui = ViewTariToolbarActionBinding.inflate(LayoutInflater.from(context), this, true)

    fun setArgs(args: TariToolbarActionArg) {
        this.setOnThrottledClickListener { args.action?.invoke() }
        this.isClickable = args.isDisabled.not()
        this.isFocusable = args.isDisabled.not()
        when {
            args.title != null -> {
                ui.toolbarText.visible()
                ui.toolbarText.isEnabled = args.isDisabled.not()
                ui.toolbarText.isClickable = false
                ui.toolbarText.isFocusable = false
                ui.toolbarText.text = args.title
            }

            args.drawable != null -> {
                ui.toolbarIcon.visible()
                ui.toolbarIcon.isEnabled = args.isDisabled.not()
                ui.toolbarIcon.setImageResource(args.drawable)
            }

            args.icon != null -> {
                ui.toolbarIcon.visible()
                ui.toolbarIcon.isEnabled = args.isDisabled.not()
                ui.toolbarIcon.setImageResource(args.icon)
            }

            args.isBack -> {
                ui.root.setStartMargin(0)
                ui.root.setEndMargin(0)
                ui.backCtaView.visible()
                ui.backCtaView.isEnabled = args.isDisabled.not()
                args.action?.let { ui.backCtaView.ui.backCtaView.setOnClickListener { it() } }
            }
        }
    }
}