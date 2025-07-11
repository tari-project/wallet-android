package com.tari.android.wallet.ui.dialog.modular.modules.shareOptions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewShareOptionBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.util.extension.setOnThrottledClickListener

class ShareOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyle, defStyleRes) {

    val ui = ViewShareOptionBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
        }
    }

    fun setArgs(args: ShareOptionArgs) {
        this.setOnThrottledClickListener { args.onClick() }
        ui.icon.setImageResource(args.icon)
        val padding = context.resources.getDimensionPixelSize(R.dimen.contact_book_share_button_padding_medium)
        ui.optionBackground.setPadding(padding, padding, padding, padding)
        ui.optionBackground.updateLayoutParams<LinearLayout.LayoutParams> {
            val backSize = context.resources.getDimensionPixelSize(R.dimen.contact_book_share_button_size_medium)
            height = backSize
            width = backSize
        }
        ui.text.text = args.title
        val textColor = if (args.isSelected) PaletteManager.getTextHeading(context) else PaletteManager.getTextBody(context)
        ui.text.setTextColor(textColor)
        val backgroundColor = if (args.isSelected) PaletteManager.getAccent(context) else PaletteManager.getBackgroundPrimary(context)
        ui.optionBackground.updateBack(backColor = backgroundColor)
        val iconColor = if (args.isSelected) PaletteManager.getBackgroundPrimary(context) else PaletteManager.getTextHeading(context)
        ui.icon.setColorFilter(iconColor)
    }
}