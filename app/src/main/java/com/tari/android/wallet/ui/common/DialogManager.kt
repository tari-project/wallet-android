package com.tari.android.wallet.ui.common

import android.content.Context
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.inProgress.ProgressDialogArgs
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog

class DialogManager {

    var context: Context? = null

    private val dialogQueue = mutableListOf<TariDialog>()

    private val currentDialog: TariDialog?
        get() = dialogQueue.lastOrNull()

    fun handleProgress(progressDialogArgs: ProgressDialogArgs) {
        if (progressDialogArgs.isShow) replace(TariProgressDialog(context!!, progressDialogArgs)) else currentDialog?.dismiss()
    }

    fun replace(dialog: TariDialog) {
        val currentLoadingDialog = currentDialog as? TariProgressDialog
        if (currentLoadingDialog != null && currentLoadingDialog.isShowing() && dialog is TariProgressDialog) {
            currentLoadingDialog.applyArgs(dialog.progressDialogArgs)
            return
        }
        val currentModularDialog = currentDialog as? ModularDialog
        val newModularDialog = dialog as? ModularDialog
        if (currentModularDialog != null && newModularDialog != null && currentModularDialog.args::class.java == newModularDialog.args::class.java
            && currentModularDialog.isShowing()
        ) {
            currentModularDialog.applyArgs(newModularDialog.args)
            return
        }

        if (newModularDialog?.args?.dialogArgs?.isRefreshing == true) {
            return
        }

        dialog.addDismissListener { dialogQueue.remove(dialog) }
        dialogQueue.add(dialog)
        dialog.show()
    }

    fun dismiss() {
        currentDialog?.dismiss()
        dialogQueue.removeLastOrNull()
    }
}