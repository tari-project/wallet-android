package com.tari.android.wallet.ui.component.tari.toolbar

import android.content.Context
import android.content.res.Resources.getSystem
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.TariToolbarBinding
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.obtain
import com.tari.android.wallet.util.extension.repopulate
import com.tari.android.wallet.util.extension.runRecycle
import com.tari.android.wallet.util.extension.setEndMargin
import com.tari.android.wallet.util.extension.setStartMargin
import com.tari.android.wallet.util.extension.visible
import kotlin.math.max

class TariToolbar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val rightArgs = mutableListOf<TariToolbarActionArg>()
    private val leftArgs = mutableListOf<TariToolbarActionArg>()

    val ui: TariToolbarBinding = TariToolbarBinding.inflate(LayoutInflater.from(context), this, false)

    init {
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
        rightArgs.repopulate(args.toMutableList())
        invalidateToolbar()
    }

    fun setLeftArgs(vararg args: TariToolbarActionArg) {
        leftArgs.repopulate(args.toMutableList())
        invalidateToolbar()
    }

    private fun invalidateToolbar() {
        ui.toolbarRightActions.removeAllViews()
        for (action in rightArgs) {
            ui.toolbarRightActions.addView(TariToolbarActionView(context).apply { setArgs(action) })
        }
        ui.toolbarLeftActions.removeAllViews()
        for (action in leftArgs) {
            ui.toolbarLeftActions.addView(TariToolbarActionView(context).apply { setArgs(action) })
        }

        // set the same margin for the both sides if toolbar actions are present
        val maxMargin = 16.dp + max(leftArgs.size, rightArgs.size) * 48.dp
        ui.toolbarTitle.setStartMargin(maxMargin - leftArgs.size * 48.dp)
        ui.toolbarTitle.setEndMargin(maxMargin - rightArgs.size * 48.dp)
    }

    fun showRightActions() {
        ui.toolbarRightActions.visible()
    }

    fun hideRightActions() {
        ui.toolbarRightActions.invisible()
    }

    val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()
}