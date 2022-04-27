package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadSpannableModule

class BackupWalletArgs(
    private val regularTitlePart: CharSequence,
    private val highlightedTitlePart: CharSequence,
    private val description: CharSequence,
    private val ctaText: Int = R.string.home_back_up_wallet_back_up_cta,
    private val dismissText: Int = R.string.home_back_up_wallet_delay_back_up_cta,
    private val backupAction: () -> Unit
) {
    fun getModular(resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(true, canceledOnTouchOutside = false), modules = listOf(
            HeadSpannableModule(regularTitlePart, highlightedTitlePart),
            BodyModule(description.toString()),
            ButtonModule(resourceManager.getString(ctaText), ButtonStyle.Normal, backupAction),
            ButtonModule(resourceManager.getString(dismissText), ButtonStyle.Close)
        )
    )
}