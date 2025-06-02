package com.tari.android.wallet.ui.screen.settings.backup.data

import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import java.io.Serializable

data class BackupOptionDto(
    val type: BackupOption,
    val isEnable: Boolean,
    val lastSuccessDate: SerializableTime?,
    val lastFailureDate: SerializableTime?,
) : Serializable // TODO Parcelable?