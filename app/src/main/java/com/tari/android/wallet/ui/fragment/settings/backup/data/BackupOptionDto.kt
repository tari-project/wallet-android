package com.tari.android.wallet.ui.fragment.settings.backup.data

import org.joda.time.DateTime
import java.io.Serializable

data class BackupOptionDto(
    val type: BackupOptions,
    val isEnable: Boolean = false,
    val lastSuccessDate: DateTime? = null,
    val lastFailureDate: DateTime? = null
) : Serializable