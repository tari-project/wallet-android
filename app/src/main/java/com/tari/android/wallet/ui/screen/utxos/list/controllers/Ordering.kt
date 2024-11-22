package com.tari.android.wallet.ui.screen.utxos.list.controllers

import com.tari.android.wallet.R

enum class Ordering(val value: Int, val textId: Int) {
    ValueAnc(0, R.string.utxos_ordering_option_size_asc),
    ValueDesc(1, R.string.utxos_ordering_option_size_desc),
    DateAnc(2, R.string.utxos_ordering_option_date_asc),
    DateDesc(3, R.string.utxos_ordering_option_date_desc),
}