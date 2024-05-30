package com.tari.android.wallet.ui.common

import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.isRefreshing
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogManager @Inject constructor() {

    private val dialogQueue = mutableListOf<TariDialog>()

    private val currentDialog: TariDialog?
        get() = dialogQueue.lastOrNull()

    fun replace(newDialog: TariDialog) {
        currentDialog.let { currentDialog ->
            when {
                currentDialog == null && newDialog.isRefreshing -> {
                    // do nothing if no dialog showing,
                    // else go further and refresh dialog
                    return
                }

                currentDialog is TariProgressDialog && newDialog is TariProgressDialog && currentDialog.isShowing() -> {
                    currentDialog.applyArgs(newDialog.progressDialogArgs)
                    return
                }

                currentDialog is ModularDialog && newDialog is ModularDialog && currentDialog.args::class.java == newDialog.args::class.java -> {
                    currentDialog.applyArgs(newDialog.args)
                    return
                }

                else -> {
                    newDialog.addDismissListener { dialogQueue.remove(newDialog) }
                    dialogQueue.add(newDialog)
                    newDialog.show()
                }
            }
        }
    }

    fun dismiss() {
        currentDialog?.dismiss()
        dialogQueue.removeLastOrNull()
    }
}
