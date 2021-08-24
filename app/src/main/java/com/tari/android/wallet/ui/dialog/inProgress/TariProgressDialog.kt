package com.tari.android.wallet.ui.dialog.inProgress

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogErrorBinding
import com.tari.android.wallet.ui.extension.setVisible

class TariProgressDialog constructor(context: Context, progressDialogArgs: ProgressDialogArgs) {

    private var dialog: Dialog

    private var ui: DialogErrorBinding

    init {
        dialog = Dialog(context, R.style.BottomSlideDialog).apply {
            setContentView(R.layout.dialog_progress)
            ui = DialogErrorBinding.bind(findViewById(R.id.root))
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setGravity(Gravity.CENTER)
            }
        }
        applyArgs(progressDialogArgs)
    }

    fun applyArgs(args: ProgressDialogArgs) = with(args) {
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(canceledOnTouchOutside)
        ui.errorDialogTitleTextView.text = title
        ui.errorDialogDescriptionTextView.text = description
        ui.errorDialogCloseView.setVisible(cancelable)
        ui.errorDialogCloseView.setOnClickListener {
            onClose()
            dismiss()
        }
    }

    fun show() = dialog.show()

    fun dismiss() = dialog.dismiss()
}