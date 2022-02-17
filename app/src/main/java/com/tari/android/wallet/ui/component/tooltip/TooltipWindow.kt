package com.tari.android.wallet.ui.component.tooltip

import android.app.ActionBar
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewTooltipLayoutBinding
import com.tari.android.wallet.ui.extension.dimenPx

class TooltipWindow(context: Context) {
    private val binding: ViewTooltipLayoutBinding = ViewTooltipLayoutBinding.inflate(LayoutInflater.from(context), null, false)
    private val tipWindow: PopupWindow = PopupWindow(context)

    fun showToolTip(anchor: View, text: String) = with(tipWindow) {
        binding.tooltipText.text = text

        tipWindow.height = ActionBar.LayoutParams.WRAP_CONTENT
        tipWindow.width = ActionBar.LayoutParams.WRAP_CONTENT
        tipWindow.isOutsideTouchable = true
        tipWindow.isTouchable = false
        tipWindow.isFocusable = false
        tipWindow.setBackgroundDrawable(BitmapDrawable())
        tipWindow.contentView = binding.root

        val screenPos = IntArray(2)
        anchor.getLocationOnScreen(screenPos)

        val contextView = binding.root
        contextView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val contentViewWidth = contentView.measuredWidth

        val positionX =
            screenPos[0] - contentViewWidth + anchor.measuredWidth / 2 + anchor.context.dimenPx(R.dimen.tooltip_offset)
        val positionY = screenPos[1] + anchor.measuredHeight / 2

        tipWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, positionX, positionY)
    }

    val isTooltipShown: Boolean
        get() = tipWindow.isShowing

    fun dismissTooltip() {
        if (tipWindow.isShowing) tipWindow.dismiss()
    }
}