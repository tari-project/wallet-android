package com.tari.android.wallet.ui.dialog.confirm

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogConfirmDefaultBinding
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.extension.string

class ConfirmDialog(context: Context, args: ConfirmDialogArgs) : TariDialog {

    private var dialog: Dialog

    init {
        with(args) {
            dialog = Dialog(context, R.style.BottomSlideDialog).apply {
                setContentView(getStyleRes(confirmStyle))
                val ui = DialogConfirmDefaultBinding.bind(findViewById(R.id.dialog_root_view))
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
                setOnDismissListener { onDismiss() }
                ui.title.text = title
                ui.description.text = description
                ui.cancelButton.text = cancelButtonText ?: context.string(R.string.common_cancel)
                ui.confirmButton.text = confirmButtonText ?: context.string(R.string.common_confirm)

                ui.cancelButton.setOnClickListener {
                    onCancel()
                    dismiss()
                }
                ui.confirmButton.setOnClickListener {
                    onConfirm()
                    dismiss()
                }
            }
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    private fun getStyleRes(confirmStyle: ConfirmStyle) = when (confirmStyle) {
        ConfirmStyle.Warning -> R.layout.dialog_confirm_warning
        else -> R.layout.dialog_confirm_default
    }

    enum class ConfirmStyle {
        Default,
        Warning
    }
}

