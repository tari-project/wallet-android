/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.notification

import android.app.KeyguardManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.tari.android.wallet.R
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.application.walletManager.WalletFileUtil
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

/**
 * Displays custom transaction notification.
 *
 * @author The Tari Development Team
 */
class CustomTxNotificationViewHolder(val context: Context, tx: Tx) : RemoteViews(context.packageName, R.layout.notification_remote_tx) {

    init {
        val user = tx.tariContact
        if (user.alias.isNotEmpty()) {
            displayTxContactAlias(user)
        } else {
            displayTxUserEmojiId(user)
        }
        displayTxMessage(tx.message)

        // amount - display just "NEW" if the screen is locked
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        val deviceIsLocked = keyguardManager?.isDeviceLocked ?: true
        when (tx.direction) {
            Tx.Direction.INBOUND -> displayIncomingTxValue(tx.amount, deviceIsLocked)
            Tx.Direction.OUTBOUND -> displayOutgoingTxValue(tx.amount, deviceIsLocked)
        }

    }

    private fun displayTxContactAlias(tariContact: TariContact) {
        setTextViewText(R.id.notification_tx_received_txt_contact_alias, tariContact.alias)
        setViewVisibility(R.id.emoji_id_view_container, View.INVISIBLE)
    }

    private fun displayTxUserEmojiId(tariContact: TariContact) {
        setTextViewText(R.id.notification_tx_received_txt_contact_alias, "")
        setViewVisibility(R.id.notification_tx_received_txt_contact_alias, View.INVISIBLE)
        setTextViewText(R.id.text_view_emoji_prefix, tariContact.walletAddress.addressPrefixEmojis())
        setTextViewText(R.id.text_view_emoji_first_part, tariContact.walletAddress.addressFirstEmojis())
        setTextViewText(R.id.text_view_emoji_last_part, tariContact.walletAddress.addressLastEmojis())
    }

    private fun displayTxMessage(message: String) {
        setTextViewText(R.id.notification_tx_received_txt_message, message)
    }

    private fun displayIncomingTxValue(amount: MicroTari, deviceIsLocked: Boolean) {
        if (deviceIsLocked) {
            setTextViewText(R.id.notification_tx_received_txt_positive_amount, context.getString(R.string.common_new_uppercase))
        } else {
            val formattedValue = "+" + WalletFileUtil.amountFormatter.format(amount.tariValue)
            setTextViewText(R.id.notification_tx_received_txt_positive_amount, formattedValue)
        }
        setViewVisibility(R.id.notification_tx_received_txt_positive_amount, View.VISIBLE)
        setViewVisibility(R.id.notification_tx_received_txt_negative_amount, View.GONE)
    }

    private fun displayOutgoingTxValue(amount: MicroTari, deviceIsLocked: Boolean) {
        if (deviceIsLocked) {
            setTextViewText(R.id.notification_tx_received_txt_negative_amount, context.getString(R.string.common_new_uppercase))
        } else {
            val formattedValue = "-" + WalletFileUtil.amountFormatter.format(amount.tariValue)
            setTextViewText(R.id.notification_tx_received_txt_negative_amount, formattedValue)
        }
        setViewVisibility(R.id.notification_tx_received_txt_negative_amount, View.VISIBLE)
        setViewVisibility(R.id.notification_tx_received_txt_positive_amount, View.GONE)
    }
}
