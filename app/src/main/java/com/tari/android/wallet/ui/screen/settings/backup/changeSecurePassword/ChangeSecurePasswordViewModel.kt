package com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword.ChangeSecurePasswordViewModel.Effect.ShowBackupPasswordError
import com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword.ChangeSecurePasswordViewModel.Effect.ShowBackupPasswordUpdated
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import java.net.UnknownHostException
import javax.inject.Inject

class ChangeSecurePasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefs: CorePrefRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSharedPrefsRepository: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    private val _effect = EffectFlow<Effect>()
    val effect = _effect.flow

    init {
        component.inject(this)
    }

    fun backToBackupSettings() {
        tariNavigator.navigate(Navigation.AllSettings.BackToBackupSettings)
    }

    fun performBackupAndUpdatePassword(password: String) {
        collectFlow(backupStateHandler.backupState.filter { it !is BackupState.BackupInProgress }.take(1)) { backupState ->
            when (backupState) {
                is BackupState.BackupUpToDate -> {
                    _effect.send(ShowBackupPasswordUpdated)
                }

                is BackupState.BackupFailed -> {
                    _effect.send(
                        ShowBackupPasswordError(
                            errorText = when {
                                backupState.backupException is UnknownHostException -> resourceManager.getString(R.string.error_no_connection_title)

                                backupState.backupException?.message != null -> resourceManager.getString(
                                    R.string.back_up_wallet_backing_up_error_desc,
                                    backupState.backupException.message!!,
                                )

                                else -> resourceManager.getString(R.string.back_up_wallet_backing_up_unknown_error)
                            }
                        )
                    )
                }

                is BackupState.BackupDisabled -> {
                    _effect.send(
                        ShowBackupPasswordError(
                            errorText = resourceManager.getString(R.string.back_up_wallet_backing_up_disabled_error),
                        )
                    )
                }

                is BackupState.BackupInProgress -> {
                    // Do nothing, backup is in progress
                }
            }
        }

        backupSharedPrefsRepository.backupPassword = password
        backupManager.backupNow()
    }

    sealed class Effect {
        data object ShowBackupPasswordUpdated : Effect()
        data class ShowBackupPasswordError(val errorText: String) : Effect()
    }
}