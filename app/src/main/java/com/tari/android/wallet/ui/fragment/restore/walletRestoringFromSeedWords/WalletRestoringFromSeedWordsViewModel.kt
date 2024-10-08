package com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.recovery.WalletRestorationState
import com.tari.android.wallet.recovery.WalletRestorationStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import io.reactivex.disposables.CompositeDisposable
import java.text.DecimalFormat
import javax.inject.Inject

class WalletRestoringFromSeedWordsViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    @Inject
    lateinit var walletRestorationStateHandler: WalletRestorationStateHandler

    private lateinit var baseNodeIterator: Iterator<BaseNodeDto>

    private val _recoveryState = MutableLiveData<RecoveryState>(RecoveryState.ConnectingToBaseNode(resourceManager))
    val recoveryState: LiveData<RecoveryState> = _recoveryState

    init {
        component.inject(this)
    }

    fun startRestoring() = launchOnIo {
        baseNodeIterator = baseNodesManager.baseNodeList.iterator()
        tryNextBaseNode()
    }

    private fun tryNextBaseNode() = launchOnIo {
        logger.i("set next base node ${baseNodeIterator.hasNext()}")
        if (baseNodeIterator.hasNext()) {
            startRestoringOnNode(baseNodeIterator.next())
        } else {
            onError(RestorationError.ConnectionFailed(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
        }
    }

    private fun startRestoringOnNode(baseNode: BaseNodeDto) {
        try {
            val result = walletManager.startRecovery(baseNode, resourceManager.getString(R.string.restore_wallet_output_message))
            if (result) {
                subscribeOnRestorationState()
                return
            } else {
                tryNextBaseNode()
            }
            onError(RestorationError.ConnectionFailed(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
        } catch (e: Throwable) {
            onError(RestorationError.RecoveryInternalError(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
        }
    }

    private fun subscribeOnRestorationState() {
        collectFlow(walletRestorationStateHandler.walletRestorationState) { state ->
            when (state) {
                is WalletRestorationState.ConnectingToBaseNode -> onProgress(RecoveryState.ConnectingToBaseNode(resourceManager))
                is WalletRestorationState.ConnectedToBaseNode -> onProgress(RecoveryState.ConnectedToBaseNode(resourceManager))
                is WalletRestorationState.ScanningRoundFailed -> onConnectionFailed(state.retryCount, state.retryLimit)
                is WalletRestorationState.ConnectionToBaseNodeFailed -> onConnectionFailed(state.retryCount, state.retryLimit)
                is WalletRestorationState.Progress -> onProgress(RecoveryState.Recovery(resourceManager, state.currentBlock, state.numberOfBlocks))
                is WalletRestorationState.RecoveryFailed -> onError(
                    RestorationError.RecoveryInternalError(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed)
                )

                is WalletRestorationState.Completed -> onSuccessRestoration()
            }
        }
    }


    private fun onConnectionFailed(retryCount: Long, retryLimit: Long) {
        if (retryCount == retryLimit) {
            compositeDisposable.dispose()
            compositeDisposable = CompositeDisposable()
            tryNextBaseNode()
        } else {
            onProgress(RecoveryState.ConnectionFailed(resourceManager, retryCount, retryLimit))
        }
    }

    private fun onError(restorationError: RestorationError) {
        if (!baseNodeIterator.hasNext()) {
            walletManager.deleteWallet()
            showModularDialog(restorationError.args.getModular(resourceManager))
        }
    }

    private fun onSuccessRestoration() {
        tariSettingsSharedRepository.hasVerifiedSeedWords = true
        navigation.postValue(Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted)
    }

    private fun onProgress(recoveryState: RecoveryState) = _recoveryState.postValue(recoveryState)

    private fun onErrorClosed() = navigation.postValue(Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreFailed)

    sealed class RestorationError(title: String, message: String, dismissAction: () -> Unit) {

        val args = SimpleDialogArgs(title = title, description = message, onClose = dismissAction)

        class ConnectionFailed(resourceManager: ResourceManager, dismissAction: () -> Unit) : RestorationError(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_error_title),
            resourceManager.getString(R.string.restore_from_seed_words_overlay_error_description_connection_failed),
            dismissAction
        )

        class RecoveryInternalError(resourceManager: ResourceManager, dismissAction: () -> Unit) : RestorationError(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_error_title),
            resourceManager.getString(R.string.restore_from_seed_words_overlay_error_description_internal_error),
            dismissAction
        )
    }

    sealed class RecoveryState(val status: String = "", val progress: String = "") {

        class ConnectingToBaseNode(resourceManager: ResourceManager) : RecoveryState(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connecting)
        )

        class ConnectionFailed(resourceManager: ResourceManager, attempt: Long, maxAttempts: Long) : RecoveryState(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connecting),
            resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connection_failed, attempt + 1, maxAttempts + 1)
        )

        class ConnectedToBaseNode(resourceManager: ResourceManager) : RecoveryState(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_status_connected)
        )

        class Recovery(resourceManager: ResourceManager, currentBlocks: Long, allBlocks: Long) : RecoveryState(
            resourceManager.getString(R.string.restore_from_seed_words_overlay_status_progress),
            DecimalFormat("#.##").format((currentBlocks.toDouble() / allBlocks) * 100) + "%"
        )
    }
}