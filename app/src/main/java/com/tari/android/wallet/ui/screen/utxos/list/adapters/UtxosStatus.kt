package com.tari.android.wallet.ui.screen.utxos.list.adapters

import com.tari.android.wallet.R

enum class UtxosStatus(val icon: Int, val textIcon: Int, val text: Int) {
    Mined(R.drawable.vector_utxos_status_mined, R.drawable.vector_utxos_status_text_mined, R.string.utxos_status_mined),
    Confirmed(R.drawable.vector_utxos_status_confirmed, R.drawable.vector_utxos_status_text_confirmed, R.string.utxos_status_confirmed)
}