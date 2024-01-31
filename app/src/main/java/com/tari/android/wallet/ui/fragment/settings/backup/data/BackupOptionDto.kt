package com.tari.android.wallet.ui.fragment.settings.backup.data

import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import java.io.Serializable

data class BackupOptionDto(
    val type: BackupOptionType,
    val isEnabled: Boolean = false,
    val lastSuccessDate: SerializableTime? = null,
    val lastFailureDate: SerializableTime? = null
) : Serializable