package com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

class WalletRestoringFromSeedWordsViewModel : CommonViewModel() {


    @Inject
    lateinit var seedPhraseRepository: SeedPhraseRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    private lateinit var baseNodeIterator: Iterator<BaseNodeDto>

    private val _recoveryState = MutableLiveData<RecoveryState>(RecoveryState.ConnectingToBaseNode(resourceManager))
    val recoveryState: LiveData<RecoveryState> = _recoveryState

    init {
        component.inject(this)
    }

    fun startRestoring() = viewModelScope.launch(Dispatchers.IO) {
        baseNodeIterator = baseNodesManager.baseNodeList.iterator()
        tryNextBaseNode()
    }

    private fun tryNextBaseNode() = viewModelScope.launch(Dispatchers.IO) {
        logger.i("set next base node ${baseNodeIterator.hasNext()}")
        if (baseNodeIterator.hasNext()) {
            startRestoringOnNode(baseNodeIterator.next())
        } else {
            onError(RestorationError.ConnectionFailed(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
        }
    }

    private fun startRestoringOnNode(baseNode: BaseNodeDto) {
        try {
            val baseNodeFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
            val result = FFIWallet.instance?.startRecovery(baseNodeFFI, resourceManager.getString(R.string.restore_wallet_output_message))
            if (result == true) {
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
        EventBus.walletRestorationState.publishSubject.subscribe {
            logger.i(it.toString())
            when (it) {
                is WalletRestorationResult.ConnectingToBaseNode -> onProgress(RecoveryState.ConnectingToBaseNode(resourceManager))
                is WalletRestorationResult.ConnectedToBaseNode -> onProgress(RecoveryState.ConnectedToBaseNode(resourceManager))
                is WalletRestorationResult.ScanningRoundFailed -> onConnectionFailed(it.retryCount, it.retryLimit)
                is WalletRestorationResult.ConnectionToBaseNodeFailed -> onConnectionFailed(it.retryCount, it.retryLimit)
                is WalletRestorationResult.Progress -> onProgress(RecoveryState.Recovery(resourceManager, it.currentBlock, it.numberOfBlocks))
                is WalletRestorationResult.RecoveryFailed -> {
                    logger.i("recovery failed ${baseNodeIterator.hasNext()}")
                    if (!baseNodeIterator.hasNext()) {
                        onError(RestorationError.RecoveryInternalError(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
                    }
                }
                is WalletRestorationResult.Completed -> onSuccessRestoration()
            }
        }.addTo(compositeDisposable)
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
        walletServiceLauncher.stopAndDelete()
        modularDialog.postValue(restorationError.args.getModular(resourceManager))
    }

    private fun onSuccessRestoration() {
        tariSettingsSharedRepository.hasVerifiedSeedWords = true
        navigation.postValue(Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted)
    }

    private fun onProgress(recoveryState: RecoveryState) = _recoveryState.postValue(recoveryState)

    private fun onErrorClosed() = navigation.postValue(Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreFailed)

    sealed class RestorationError(title: String, message: String, dismissAction: () -> Unit) {

        val args = ErrorDialogArgs(title, message, onClose = dismissAction)

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