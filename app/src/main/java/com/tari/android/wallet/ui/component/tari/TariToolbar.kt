package com.tari.android.wallet.ui.component.tari

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.TariToolbarBinding
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.visible

class TariToolbar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    var backPressedAction: () -> Unit = { (context as? Activity)?.onBackPressed() }
    var rightAction: () -> Unit = { }

    init {
        val ui = TariToolbarBinding.inflate(LayoutInflater.from(context), this, false)
        addView(ui.root)

        ui.backCtaView.setOnThrottledClickListener { backPressedAction.invoke() }

        obtain(attrs, R.styleable.TariToolbar).runRecycle {
            ui.toolbarTitle.text = getString(R.styleable.TariToolbar_text)
            getString(R.styleable.TariToolbar_rightText)?.let { setupRightButton(ui, it) }
            getDrawable(R.styleable.TariToolbar_rightIcon)?.let { setupRightIcon(ui, it) }
        }
    }

    private fun setupRightButton(ui: TariToolbarBinding, newText: String) = with(ui.toolbarRightText) {
        visible()
        text = newText
        setOnThrottledClickListener { rightAction() }
    }

    private fun setupRightIcon(ui: TariToolbarBinding, icon: Drawable) = with(ui.toolbarRightIcon) {
        visible()
        setImageDrawable(icon)
        setOnThrottledClickListener { rightAction() }
    }
}