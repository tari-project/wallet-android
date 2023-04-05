package com.tari.android.wallet.ui.fragment.contact_book.root.share

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewShareOptionBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener

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

    fun setArgs(args: ShareOptionArgs, size: Size = Size.Medium) {
        this.setOnThrottledClickListener { args.onClick() }
        ui.icon.setImageResource(args.icon)
        val padding = context.resources.getDimensionPixelSize(size.padding)
        ui.optionBackground.setPadding(padding, padding, padding, padding)
        ui.optionBackground.updateLayoutParams<LinearLayout.LayoutParams> {
            val backSize = context.resources.getDimensionPixelSize(size.value)
            height = backSize
            width = backSize
        }
        ui.text.text = args.title
        val paletteManager = PaletteManager()
        val textColor = if (args.isSelected) paletteManager.getTextHeading(context) else paletteManager.getTextBody(context)
        ui.text.setTextColor(textColor)
        val backgroundColor = if (args.isSelected) paletteManager.getPurpleBrand(context) else paletteManager.getBackgroundPrimary(context)
        ui.optionBackground.updateBack(backColor = backgroundColor)
        val iconColor = if (args.isSelected) paletteManager.getBackgroundPrimary(context) else paletteManager.getTextHeading(context)
        ui.icon.setColorFilter(iconColor)
    }

    enum class Size(val value: Int, val padding: Int) {
        Big(R.dimen.contact_book_share_button_size_big, R.dimen.contact_book_share_button_padding_big),
        Medium(R.dimen.contact_book_share_button_size_medium, R.dimen.contact_book_share_button_padding_medium)
    }
}