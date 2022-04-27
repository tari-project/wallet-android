package com.tari.android.wallet.ui.dialog.modular

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody.CustomBaseNodeBodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody.CustomBaseNodeBodyModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.head.*
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModuleView
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQRCodeModuleView
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule


open class ModularDialog(val context: Context) : TariDialog {

    constructor(context: Context, args: ModularDialogArgs) : this(context) {
        applyArgs(args)
    }

    val dialog: Dialog = Dialog(context, R.style.BottomSlideDialog).apply {
        setContentView(R.layout.dialog_base)
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            it.setGravity(Gravity.BOTTOM)
        }
    }

    fun applyArgs(args: ModularDialogArgs) {
        with(dialog) {
            setCancelable(args.dialogArgs.cancelable)
            setCanceledOnTouchOutside(args.dialogArgs.canceledOnTouchOutside)
            setOnDismissListener { args.dialogArgs.onDismiss() }
        }
        updateModules(args.modules)
    }

    private fun updateModules(modules: List<IDialogModule>) {
        val root = dialog.findViewById<LinearLayout>(R.id.dialog_root_view)
        for (module in modules) {
            module.dismissAction = dialog::dismiss
            val view = when(module) {
                is HeadModule -> HeadModuleView(context, module)
                is HeadSpannableModule -> HeadSpannableModuleView(context, module)
                is HeadBoldSpannableModule -> HeadBoldSpannableModuleView(context, module)
                is ImageModule -> ImageModuleView(context, module)
                is BodyModule -> BodyModuleView(context, module)
                is ButtonModule -> ButtonModuleView(context, module) { dialog.dismiss() }
                is CustomBaseNodeBodyModule -> CustomBaseNodeBodyModuleView(context, module)
                is ShareQrCodeModule -> ShareQRCodeModuleView(context, module)
                else -> View(context)
            }
            root.addView(view)
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}