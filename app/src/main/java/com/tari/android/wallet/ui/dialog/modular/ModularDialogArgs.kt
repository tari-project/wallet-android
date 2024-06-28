package com.tari.android.wallet.ui.dialog.modular

/**
 * @param dialogId should be unique for each dialog to prevent creating new instances of the same dialog, but refresh existing ones
 */
data class ModularDialogArgs(
    val dialogArgs: DialogArgs = DialogArgs(),
    val modules: List<IDialogModule> = emptyList(),
    val dialogId: Int = DialogId.NO_ID,
) {
    object DialogId {
        const val NO_ID = -1
        const val CONNECTION_STATUS = 601
        const val DEBUG_MENU = 602
    }
}
