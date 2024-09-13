package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.updateLayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewShareOptionBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener

class RoundButtonWithIconView @JvmOverloads constructor(
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

    fun setArgs(text: String, @DrawableRes icon: Int, action: () -> Unit, size: Size = Size.Medium, isSelected: Boolean = false) {
        this.setOnThrottledClickListener { action() }
        ui.icon.setImageResource(icon)
        val padding = context.resources.getDimensionPixelSize(size.padding)
        ui.optionBackground.setPadding(padding, padding, padding, padding)
        ui.optionBackground.updateLayoutParams<LinearLayout.LayoutParams> {
            val backSize = context.resources.getDimensionPixelSize(size.value)
            height = backSize
            width = backSize
        }
        ui.text.text = text
        val textColor = if (isSelected) PaletteManager.getTextHeading(context) else PaletteManager.getTextBody(context)
        ui.text.setTextColor(textColor)
        val backgroundColor = if (isSelected) PaletteManager.getPurpleBrand(context) else PaletteManager.getBackgroundPrimary(context)
        ui.optionBackground.updateBack(backColor = backgroundColor)
        val iconColor = if (isSelected) PaletteManager.getBackgroundPrimary(context) else PaletteManager.getTextHeading(context)
        ui.icon.setColorFilter(iconColor)
    }

    enum class Size(val value: Int, val padding: Int) {
        Big(R.dimen.contact_book_share_button_size_big, R.dimen.contact_book_share_button_padding_big),
        Medium(R.dimen.contact_book_share_button_size_medium, R.dimen.contact_book_share_button_padding_medium)
    }
}