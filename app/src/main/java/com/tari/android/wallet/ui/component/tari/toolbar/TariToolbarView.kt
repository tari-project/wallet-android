package com.tari.android.wallet.ui.component.tari.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.ViewTariToolbarActionBinding
import com.tari.android.wallet.ui.extension.visible

class TariToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyle, defStyleRes) {

    private val ui = ViewTariToolbarActionBinding.inflate(LayoutInflater.from(context), this, true)

    fun setArgs(args: TariToolbarActionArg) {
        when {
            args.title != null -> {
                ui.toolbarText.visible()
                ui.toolbarText.text = args.title
                args.action?.let { ui.toolbarText.setOnClickListener { it() } }
            }

            args.drawable != null -> {
                ui.toolbarIcon.visible()
                ui.toolbarIcon.setImageResource(args.drawable)
                args.action?.let { ui.toolbarIcon.setOnClickListener { it() } }
            }

            args.icon != null -> {
                ui.toolbarIcon.visible()
                ui.toolbarIcon.setImageResource(args.icon)
                args.action?.let { ui.toolbarIcon.setOnClickListener { it() } }
            }

            args.isBack -> {
                ui.backCtaView.visible()
                args.action?.let { ui.backCtaView.ui.backCtaView.setOnClickListener { it() } }
            }
        }
    }
}