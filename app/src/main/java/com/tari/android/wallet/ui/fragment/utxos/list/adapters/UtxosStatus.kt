package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.R

enum class UtxosStatus(val icon: Int, val text: Int) {
    Mined(R.drawable.ic_utxos_status_mined, R.string.utxos_status_mined),
    Confirmed(R.drawable.ic_utxos_status_confirmed, R.string.utxos_status_confirmed),
    Unconfirmed(R.drawable.ic_utxos_status_unconfirmed, R.string.utxos_status_unconfirmed)
}