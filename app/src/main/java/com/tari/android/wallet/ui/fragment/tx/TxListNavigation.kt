package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User

sealed class TxListNavigation {
    class ToTxDetails(val tx: Tx) : TxListNavigation()

    object ToTTLStore : TxListNavigation()

    object ToAllSettings : TxListNavigation()

    object ToUtxos : TxListNavigation()

    class ToSendTariToUser(val user: User) : TxListNavigation()
}