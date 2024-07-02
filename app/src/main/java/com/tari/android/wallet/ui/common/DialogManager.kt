package com.tari.android.wallet.ui.common

import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogManager @Inject constructor() {

    private val dialogQueue = mutableListOf<ModularDialog>()

    fun replace(newDialog: ModularDialog) {
        if (isDialogShowing(newDialog.args.dialogId)) {
            getDialog(newDialog.args.dialogId)?.applyArgs(newDialog.args)
        } else {
            newDialog.addDismissListener { dialogQueue.remove(newDialog) }
            dialogQueue.add(newDialog)
            newDialog.show()
        }
    }

    /**
     * Dismisses the dialog with the given dialogId. If dialogId is [DialogId.NO_ID], the last dialog in the queue will be dismissed.
     */
    fun dismiss(dialogId: Int = DialogId.NO_ID) {
        val dialogToDismiss = if (isDialogShowing(dialogId)) {
            getDialog(dialogId)
        } else {
            dialogQueue.lastOrNull()
        }
        dialogToDismiss?.dismiss()
        dialogQueue.remove(dialogToDismiss)
    }

    fun isDialogShowing(dialogId: Int) = dialogId != DialogId.NO_ID && dialogQueue.any { it.args.dialogId == dialogId }

    private fun getDialog(dialogId: Int): ModularDialog? =
        if (dialogId != DialogId.NO_ID) dialogQueue.firstOrNull { it.args.dialogId == dialogId } else null
}
