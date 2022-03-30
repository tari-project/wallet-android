package com.tari.android.wallet.ui.dialog.addCustomBaseNode

import android.content.Context
import android.view.LayoutInflater
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewCustomBaseNodeFullDescriptionBinding
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialog
import yat.android.ui.extension.HtmlHelper

class AddCustomBaseNodeDialog(context: Context, args: AddCustomBaseNodeDialogArgs) : ConfirmDialog(context, args.dialogArgs) {

    init {
        val view = ViewCustomBaseNodeFullDescriptionBinding.inflate(LayoutInflater.from(context))
        val peer = args.baseNodeInfo.publicKeyHex + "::" + args.baseNodeInfo.address
        val pattern = HtmlHelper.getSpannedText(context.getString(R.string.home_custom_base_node_full_description, args.baseNodeInfo.name, peer))
        view.descriptionText.text = pattern
        ui.dialogRootView.addView(view.root, 2)
    }
}