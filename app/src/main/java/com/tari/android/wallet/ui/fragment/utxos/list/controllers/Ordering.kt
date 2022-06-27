package com.tari.android.wallet.ui.fragment.utxos.list.controllers

import com.tari.android.wallet.R

enum class Ordering(val textId: Int) {
    ValueDesc(R.string.utxos_ordering_option_size_desc),
    ValueAnc(R.string.utxos_ordering_option_size_asc),
    DateDesc(R.string.utxos_ordering_option_date_desc),
    DateAnc(R.string.utxos_ordering_option_date_asc),
}