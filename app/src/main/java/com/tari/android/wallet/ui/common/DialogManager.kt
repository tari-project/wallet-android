package com.tari.android.wallet.ui.common

import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogManager @Inject constructor() {

    private val dialogQueue = mutableListOf<ModularDialog>()

    private val currentDialog: ModularDialog?
        get() = dialogQueue.lastOrNull()

    fun replace(newDialog: ModularDialog) {
        if (isDialogShowing(newDialog.args.dialogId)) {
            getDialog(newDialog.args.dialogId)?.applyArgs(newDialog.args)
        } else {
            newDialog.addDismissListener { dialogQueue.remove(newDialog) }
            dialogQueue.add(newDialog)
            newDialog.show()
        }
    }

    fun dismiss() {
        currentDialog?.dismiss()
        dialogQueue.removeLastOrNull()
    }

    fun isDialogShowing(dialogId: Int) = dialogId != DialogId.NO_ID && dialogQueue.any { it.args.dialogId == dialogId }

    private fun getDialog(dialogId: Int): ModularDialog? =
        if (dialogId != DialogId.NO_ID) dialogQueue.firstOrNull { it.args.dialogId == dialogId } else null
}
