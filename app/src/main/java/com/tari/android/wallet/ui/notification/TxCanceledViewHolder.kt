package com.tari.android.wallet.ui.notification

import android.app.KeyguardManager
import android.content.Context
import android.widget.RemoteViews
import com.tari.android.wallet.R
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.util.WalletUtil

class TxCanceledViewHolder(context: Context, tx: CancelledTx) :
    RemoteViews(context.packageName, R.layout.tx_canceled_notification) {

    init {
        // amount - display just "NEW" if the screen is locked
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        val deviceIsLocked = keyguardManager?.isDeviceLocked ?: true
        val amount =
            if (deviceIsLocked) context.getString(R.string.common_new_uppercase)
            else WalletUtil.amountFormatter.format(tx.amount.tariValue)
        setTextViewText(R.id.notification_tx_canceled_amount_text_view, amount)
    }

}
