package com.tari.android.wallet.infrastructure.backup

import com.tari.android.wallet.ffi.Base58

data class BackupUtxos(val utxos: List<String>?, val sourceBase58: Base58)