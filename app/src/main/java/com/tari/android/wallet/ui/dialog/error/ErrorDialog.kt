package com.tari.android.wallet.ui.dialog.error

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogErrorBinding

class ErrorDialog constructor(context: Context, errorDialogArgs: ErrorDialogArgs) {

    @Deprecated("Use through viewModel or via errorDialogArgs")
    constructor(
        context: Context,
        title: CharSequence,
        description: CharSequence,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        onClose: () -> Unit = {}
    ) : this(context, ErrorDialogArgs(title, description, cancelable, canceledOnTouchOutside, onClose))

    private var dialog: Dialog

    init {
        with(errorDialogArgs) {
            dialog = Dialog(context, R.style.BottomSlideDialog).apply {
                setContentView(R.layout.dialog_error)
                val ui = DialogErrorBinding.bind(findViewById(R.id.root))
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
                ui.errorDialogTitleTextView.text = title
                ui.errorDialogDescriptionTextView.text = description
                ui.errorDialogCloseView.setOnClickListener { dismiss() }
                setOnDismissListener { onClose() }
            }
        }
    }

    fun show() = dialog.show()
}