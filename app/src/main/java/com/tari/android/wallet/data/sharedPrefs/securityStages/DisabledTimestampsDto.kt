package com.tari.android.wallet.data.sharedPrefs.securityStages

import java.util.Calendar

class DisabledTimestampsDto(val timestamps: MutableMap<WalletSecurityStage, Calendar> = mutableMapOf())