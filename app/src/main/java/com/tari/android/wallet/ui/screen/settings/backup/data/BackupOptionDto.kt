package com.tari.android.wallet.ui.screen.settings.backup.data

import android.os.Parcelable
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupOptionDto(
    val type: BackupOption,
    val isEnabled: Boolean,
    val lastSuccessDate: SerializableTime?,
    val lastFailureDate: SerializableTime?,
) : Parcelable