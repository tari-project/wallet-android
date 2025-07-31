package com.tari.android.wallet.ui.screen.restore.walletRestoring

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.recovery.WalletRestorationState
import com.tari.android.wallet.data.recovery.WalletRestorationStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.util.extension.switchToMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

class WalletRestoringViewModel : CommonViewModel() {

    @Inject
    lateinit var walletRestorationStateHandler: WalletRestorationStateHandler

    private val _recoveryState = MutableStateFlow<RestorationState>(RestorationState.ConnectingToBaseNode(resourceManager))
    val recoveryState = _recoveryState.asStateFlow()

    private val _keepScreenAwake = MutableStateFlow(sharedPrefsRepository.keepScreenAwakeWhenRestore)
    val keepScreenAwake = _keepScreenAwake.asStateFlow()

    private val _effect = EffectFlow<Effect>()
    val effect = _effect.flow

    init {
        component.inject(this)

        startRestoring()
    }

    fun startRestoring() = launchOnIo {
        switchToMain { _effect.send(Effect.StartService) }
        subscribeOnRestorationState()
        if (!walletRestorationStateHandler.isWalletRestoring()) {
            startRecovery()
        }
    }

    fun showResetFlowDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.restore_from_seed_words_cancel_dialog_title)),
            BodyModule(resourceManager.getString(R.string.restore_from_seed_words_cancel_dialog_description)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                viewModelScope.launch(Dispatchers.Main) {
                    cancelRecovery()
                    hideDialog()
                }
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                hideDialog()
            },
        )
    }

    private fun startRecovery() {
        runCatching {
            val startedSuccessfully = walletManager.startRecovery()
            if (!startedSuccessfully) {
                onError(RestorationError.ConnectionFailed(resourceManager, this@WalletRestoringViewModel::cancelRecovery))
            }
        }.onFailure {
            onError(RestorationError.RecoveryInternalError(resourceManager, this@WalletRestoringViewModel::cancelRecovery))
        }
    }

    private fun subscribeOnRestorationState() {
        collectFlow(walletRestorationStateHandler.walletRestorationState) { state ->
            launchOnMain {
                when (state) {
                    is WalletRestorationState.NotStarted -> {
                        updateState(RestorationState.ConnectingToBaseNode(resourceManager))
                    }

                    is WalletRestorationState.ScanningRoundFailed -> {
                        onConnectionFailed(state.retryCount, state.retryLimit)
                    }

                    is WalletRestorationState.Progress -> {
                        updateState(RestorationState.Recovery(resourceManager, state.currentBlock, state.numberOfBlocks))
                    }

                    is WalletRestorationState.Completed -> {
                        onSuccessRestoration()
                    }
                }
            }
        }
    }

    private suspend fun onConnectionFailed(retryCount: Long, retryLimit: Long) = switchToMain {
        updateState(RestorationState.ConnectionFailed(resourceManager, retryCount, retryLimit))
    }

    private fun onError(restorationError: RestorationError) {
        showModularDialog(
            restorationError.args.copy(
                onClose = { cancelRecovery() }
            ).getModular(resourceManager)
        )
    }

    private suspend fun onSuccessRestoration() = switchToMain {
        _effect.send(Effect.StopService)

        tariSettingsSharedRepository.hasVerifiedSeedWords = true
        walletManager.onWalletRestored()
        tariNavigator.navigate(Navigation.SplashScreen(clearTop = false))
    }

    private fun updateState(recoveryState: RestorationState) = _recoveryState.update { recoveryState }

    private fun cancelRecovery() {
        launchOnMain { _effect.send(Effect.StopService) }
        walletManager.deleteWallet()
        tariNavigator.navigate(Navigation.SplashScreen())
    }

    fun toggleKeepScreenAwake(checked: Boolean) {
        sharedPrefsRepository.keepScreenAwakeWhenRestore = checked
        _keepScreenAwake.value = checked
    }

    sealed class RestorationError(title: String, message: String, dismissAction: () -> Unit) {

        val args = SimpleDialogArgs(title = title, description = message, onClose = dismissAction)

        class ConnectionFailed(resourceManager: ResourceManager, dismissAction: () -> Unit) : RestorationError(
            title = resourceManager.getString(R.string.restore_from_seed_words_overlay_error_title),
            message = resourceManager.getString(R.string.restore_from_seed_words_overlay_error_description_connection_failed),
            dismissAction = dismissAction,
        )

        class RecoveryInternalError(resourceManager: ResourceManager, dismissAction: () -> Unit) : RestorationError(
            title = resourceManager.getString(R.string.restore_from_seed_words_overlay_error_title),
            message = resourceManager.getString(R.string.restore_from_seed_words_overlay_error_description_internal_error),
            dismissAction = dismissAction,
        )
    }

    sealed class RestorationState(val status: String = "", val progress: String = "") {

        class ConnectingToBaseNode(resourceManager: ResourceManager) : RestorationState(
            status = resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connecting)
        )

        class ConnectionFailed(resourceManager: ResourceManager, attempt: Long, maxAttempts: Long) : RestorationState(
            status = resourceManager.getString(R.string.restore_from_seed_words_overlay_status_reconnecting),
            progress = resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connection_failed, attempt, maxAttempts),
        )

        class Recovery(resourceManager: ResourceManager, currentBlocks: Long, allBlocks: Long) : RestorationState(
            status = resourceManager.getString(R.string.restore_from_seed_words_overlay_status_progress),
            progress = DecimalFormat("#.##").format(if (allBlocks == 0L) 0f else (currentBlocks.toDouble() / allBlocks) * 100) + "%",
        )
    }

    sealed class Effect {
        data object StartService : Effect()
        data object StopService : Effect()
    }
}