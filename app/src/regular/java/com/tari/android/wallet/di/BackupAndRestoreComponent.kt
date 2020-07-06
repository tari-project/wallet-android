package com.tari.android.wallet.di

import com.tari.android.wallet.ui.activity.restore.WalletRestoreActivity
import com.tari.android.wallet.ui.fragment.restore.ChooseRestoreOptionFragment
import com.tari.android.wallet.ui.fragment.restore.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.fragment.settings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.WalletBackupSettingsFragment
import dagger.Component
import javax.inject.Scope

@BackupAndRestoreScope
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [BackupAndRestoreModule::class]
)
interface BackupAndRestoreComponent {

    fun inject(activity: WalletRestoreActivity)

    fun inject(fragment: ChooseRestoreOptionFragment)
    fun inject(fragment: EnterRestorationPasswordFragment)
    fun inject(fragment: AllSettingsFragment)
    fun inject(fragment: WalletBackupSettingsFragment)
    fun inject(fragment: ChangeSecurePasswordFragment)
    fun inject(fragment: EnterCurrentPasswordFragment)

}

@Scope
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class BackupAndRestoreScope
