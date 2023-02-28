package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.TariToolbarBinding
import com.tari.android.wallet.ui.extension.*

class TariToolbar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    var backPressedAction: () -> Unit
        get() = ui.backCtaView.backPressedAction
        set(value) {
            ui.backCtaView.backPressedAction = value
        }

    var rightAction: () -> Unit = { }

    val ui: TariToolbarBinding

    init {
        ui = TariToolbarBinding.inflate(LayoutInflater.from(context), this, false)
        addView(ui.root)

        ui.backCtaView.setOnThrottledClickListener { backPressedAction.invoke() }

        obtain(attrs, R.styleable.TariToolbar).runRecycle {
            setText(getString(R.styleable.TariToolbar_text))
            getString(R.styleable.TariToolbar_rightText)?.let { setupRightButton(it) }
            getDrawable(R.styleable.TariToolbar_rightIcon)?.let { setupRightIcon(it) }
            getBoolean(R.styleable.TariToolbar_isRoot, false).let { ui.backCtaView.setVisible(!it) }
        }
    }

    fun setText(text: String?) {
        ui.toolbarTitle.text = text
    }

    fun setupRightButton(newText: String) = with(ui.toolbarRightText) {
        visible()
        text = newText
        setOnThrottledClickListener { rightAction() }
    }

    fun clearRightIcon() {
        ui.toolbarRightIcon.gone()
        ui.toolbarRightText.gone()
    }

    fun setupRightIcon(icon: Drawable) = with(ui.toolbarRightIcon) {
        visible()
        setImageDrawable(icon)
        setOnThrottledClickListener { rightAction() }
    }
}