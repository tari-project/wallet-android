package com.tari.android.wallet.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.tari.android.wallet.R

class BottomSlideDialog private constructor(private val dialog: Dialog) {

    constructor(
        context: Context,
        layoutId: Int,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        dismissViewId: Int? = null
    ) : this(
        Dialog(context, R.style.BottomSlideDialog).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(layoutId)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(canceledOnTouchOutside)
            window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.BOTTOM)
            dismissViewId?.let { findViewById<View>(it).setOnClickListener { dismiss() } }
        })

    fun <T : View> findViewById(id: Int): T = dialog.findViewById(id)

    fun show(): Dialog {
        dialog.show()
        return dialog
    }

    fun dismiss() = dialog.dismiss()

}
