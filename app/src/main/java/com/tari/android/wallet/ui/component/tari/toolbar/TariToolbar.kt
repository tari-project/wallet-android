package com.tari.android.wallet.ui.component.tari.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.TariToolbarBinding
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle
import com.tari.android.wallet.ui.extension.visible


class TariToolbar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private var rightArgs = mutableListOf<TariToolbarActionArg>()
    private var leftArgs = mutableListOf<TariToolbarActionArg>()

    val ui: TariToolbarBinding

    init {
        ui = TariToolbarBinding.inflate(LayoutInflater.from(context), this, false)
        addView(ui.root)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        leftArgs.add(TariToolbarActionArg(isBack = true))

        obtain(attrs, R.styleable.TariToolbar).runRecycle {
            setText(getString(R.styleable.TariToolbar_text))
            getBoolean(R.styleable.TariToolbar_isRoot, false).let { if (it) leftArgs.clear() }
        }
        invalidateToolbar()
    }

    fun setOnBackPressedAction(action: () -> Unit) {
        leftArgs.clear()
        leftArgs.add(TariToolbarActionArg(isBack = true) { action.invoke() })
    }

    fun setText(text: String?) {
        ui.toolbarTitle.text = text
    }

    fun setRightArgs(vararg args: TariToolbarActionArg) {
        rightArgs = args.toMutableList()
        invalidateToolbar()
    }

    fun setLeftArgs(vararg args: TariToolbarActionArg) {
        leftArgs = args.toMutableList()
        invalidateToolbar()
    }

    private fun invalidateToolbar() {
        ui.toolbarRightActions.removeAllViews()
        for (action in rightArgs) {
            ui.toolbarRightActions.addView(TariToolbarView(context).apply { setArgs(action) })
        }
        ui.toolbarLeftActions.removeAllViews()
        for (action in leftArgs) {
            ui.toolbarLeftActions.addView(TariToolbarView(context).apply { setArgs(action) })
        }
    }

    fun showRightActions() {
        ui.toolbarRightActions.visible()
    }

    fun hideRightActions() {
        ui.toolbarRightActions.gone()
    }

    fun showLeftActions() {
        ui.toolbarLeftActions.visible()
    }

    fun hideLeftActions() {
        ui.toolbarLeftActions.gone()
    }
}