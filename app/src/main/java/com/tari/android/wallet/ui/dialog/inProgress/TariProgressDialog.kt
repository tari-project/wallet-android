package com.tari.android.wallet.ui.dialog.inProgress

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogProgressBinding
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.extension.setVisible

@Deprecated("Use modular dialog")
class TariProgressDialog constructor(val context: Context, val progressDialogArgs: ProgressDialogArgs) : TariDialog {

    var dialog: Dialog

    private var ui: DialogProgressBinding

    init {
        dialog = Dialog(context, R.style.BottomSlideDialog).apply {
            setContentView(R.layout.dialog_progress)
            ui = DialogProgressBinding.bind(findViewById(R.id.root))
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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
        ui.errorDialogCloseView.text = closeButtonText ?: context.getString(R.string.common_close)
        ui.errorDialogCloseView.setVisible(cancelable)
        ui.errorDialogCloseView.setOnClickListener {
            onClose()
            dismiss()
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing

    override fun addDismissListener(onDismiss: () -> Unit) = Unit
}