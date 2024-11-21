package com.tari.android.wallet.ui.screen.restore.inputSeedWords

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewRecoveryWordBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.util.extension.setVisible

class WordTextView : FrameLayout {

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

    lateinit var ui: ViewRecoveryWordBinding

    fun init() {
        ui = ViewRecoveryWordBinding.inflate(LayoutInflater.from(context), this, false).apply {
            text.background = null
            text.setPadding(0, 0, 0, 0)
        }
        updateState(isFocused = false, isValid = true)
        addView(ui.root)
    }

    fun updateState(isFocused: Boolean, isValid: Boolean) {
        ui.removeView.setVisible(!isFocused)
        val background = when {
            isFocused -> null
            isValid -> R.drawable.vector_restoring_seed_phrase_word_bg
            else -> R.drawable.vector_restoring_seed_phrase_word_bg_error
        }
        ui.root.background = background?.let { ContextCompat.getDrawable(context, it) }

        val textColor = if (isFocused || isValid) PaletteManager.getTextHeading(context) else PaletteManager.getRed(context)
        ui.text.setTextColor(textColor)

        val deleteTintColor = ContextCompat.getDrawable(context, if (isFocused || isValid) R.drawable.vector_close else R.drawable.vector_close_error)
        ui.removeView.setImageDrawable(deleteTintColor)
    }
}