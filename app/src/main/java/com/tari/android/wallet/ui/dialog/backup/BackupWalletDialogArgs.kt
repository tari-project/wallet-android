package com.tari.android.wallet.ui.dialog.backup

import com.tari.android.wallet.R

class BackupWalletDialogArgs(
    val regularTitlePart: CharSequence,
    val highlightedTitlePart: CharSequence,
    val description: CharSequence,
    val ctaText: Int = R.string.home_back_up_wallet_back_up_cta,
    val dismissText: Int = R.string.home_back_up_wallet_delay_back_up_cta,
    val backupAction: () -> Unit
)