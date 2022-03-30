package com.tari.android.wallet.ui.dialog.addCustomBaseNode

import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs

class AddCustomBaseNodeDialogArgs(
    val baseNodeInfo: BaseNodeDto,
    val dialogArgs: ConfirmDialogArgs
)