package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact

sealed class TxListNavigation {

    object ToSplashScreen : TxListNavigation()

    class ToTxDetails(val tx: Tx) : TxListNavigation()

    object ToTTLStore : TxListNavigation()

    object ToAllSettings : TxListNavigation()

    object ToUtxos : TxListNavigation()

    class ToSendTariToUser(val user: IContact) : TxListNavigation()
}