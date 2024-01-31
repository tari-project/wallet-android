package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import android.os.Parcelable
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType
import kotlinx.parcelize.Parcelize

internal const val EXTRA_PARAMETERS_KEY = "EXTRA_PARAMETERS_KEY"

object EnterRestorationPasswordModel {
    @Parcelize
    data class Parameters(val selectedOptionType: BackupOptionType) : Parcelable
}