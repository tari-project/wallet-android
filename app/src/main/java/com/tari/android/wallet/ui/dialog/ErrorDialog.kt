package com.tari.android.wallet.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.tari.android.wallet.R

class ErrorDialog private constructor(private val dialog: Dialog) {

    constructor(
        context: Context,
        title: CharSequence,
        description: CharSequence,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        onClose: () -> Unit = {},
        closeButtonTextResourceId: Int = R.string.common_close
    ) : this(
        Dialog(context, R.style.BottomSlideDialog).apply {
            setContentView(R.layout.dialog_error)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setGravity(Gravity.BOTTOM)
            }
            setCancelable(cancelable)
            setCanceledOnTouchOutside(canceledOnTouchOutside)
            findViewById<TextView>(R.id.error_dialog_title_text_view).text = title
            findViewById<TextView>(R.id.error_dialog_description_text_view).text = description
            findViewById<TextView>(R.id.error_dialog_close_view).text = context.resources.getString(closeButtonTextResourceId)
            findViewById<View>(R.id.error_dialog_close_view).setOnClickListener {
                onClose()
                dismiss()
            }
        })

    fun show() = dialog.show()

}
