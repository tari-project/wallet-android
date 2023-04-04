package com.tari.android.wallet.ui.fragment.contact_book.root.share

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    fun setArgs(args: ShareOptionArgs) {
        this.setOnThrottledClickListener { args.onClick() }
        ui.icon.setImageResource(args.icon)
        ui.text.text = args.title
        val paletteManager = PaletteManager()
        val textColor = if (args.isSelected) paletteManager.getTextHeading(context) else paletteManager.getTextBody(context)
        ui.text.setTextColor(textColor)
        val backgroundColor = if (args.isSelected) paletteManager.getPurpleBrand(context) else paletteManager.getBackgroundPrimary(context)
        ui.optionBackground.updateBack(backColor = backgroundColor)
        val iconColor = if (args.isSelected) paletteManager.getBackgroundPrimary(context) else paletteManager.getTextHeading(context)
        ui.icon.setColorFilter(iconColor)
    }
}