package com.tari.android.wallet.ui.extension

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.di.DaggerBackupAndRestoreComponent

val FragmentActivity.backupAndRestoreComponent
    get() = DaggerBackupAndRestoreComponent.builder().applicationComponent(appComponent).build()

val Fragment.backupAndRestoreComponent get() = requireActivity().backupAndRestoreComponent
