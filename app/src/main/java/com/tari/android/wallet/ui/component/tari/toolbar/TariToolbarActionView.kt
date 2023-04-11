package com.tari.android.wallet.ui.component.tari.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewTariToolbarActionBinding
import com.tari.android.wallet.ui.extension.setEndMargin
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.setStartMargin
import com.tari.android.wallet.ui.extension.visible

class TariToolbarActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyle, defStyleRes) {

    private val ui = ViewTariToolbarActionBinding.inflate(LayoutInflater.from(context), this, true)

    fun setArgs(args: TariToolbarActionArg, isRight: Boolean) {
        this.setOnThrottledClickListener { args.action?.invoke() }
        this.isClickable = args.isDisabled.not()
        this.isFocusable = args.isDisabled.not()
        setMargins(ui.root, isRight)
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

    private fun setMargins(view: View, isRight: Boolean) {
        view.setStartMargin(if (isRight) 0 else context.resources.getDimensionPixelSize(R.dimen.common_horizontal_margin))
        view.setEndMargin(if (!isRight) 0 else context.resources.getDimensionPixelSize(R.dimen.common_horizontal_margin))
    }
}