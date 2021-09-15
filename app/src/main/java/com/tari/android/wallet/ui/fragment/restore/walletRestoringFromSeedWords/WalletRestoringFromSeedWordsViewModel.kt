package com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

internal class WalletRestoringFromSeedWordsViewModel() : CommonViewModel() {


    @Inject
    lateinit var seedPhraseRepository: SeedPhraseRepository

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var baseNodes: BaseNodes

    private lateinit var baseNodeIterator: Iterator<BaseNodeDto>

    private val _navigation = SingleLiveEvent<WalletRestoringFromSeedWordsNavigation>()
    val navigation: LiveData<WalletRestoringFromSeedWordsNavigation> = _navigation

    private val _recoveryState = MutableLiveData<RecoveryState>(RecoveryState.ConnectingToBaseNode(resourceManager))
    val recoveryState: LiveData<RecoveryState> = _recoveryState

    init {
        component?.inject(this)
    }

    fun startRestoring() = viewModelScope.launch(Dispatchers.IO) {
        baseNodeIterator = baseNodes.baseNodeList.iterator()
        tryNextBaseNode()
    }

    private fun tryNextBaseNode() = viewModelScope.launch(Dispatchers.IO) {
        if (baseNodeIterator.hasNext()) {
            startRestoringOnNode(baseNodeIterator.next())
        } else {
            onError(RestorationError.ConnectionFailed(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
        }
    }

    private fun startRestoringOnNode(baseNode: BaseNodeDto) {
        try {
            val baseNodeFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
            val result = FFIWallet.instance?.startRecovery(baseNodeFFI)
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
        EventBus.walletRestorationState.publishSubject.skip(1).subscribe {
            when (it) {
                is WalletRestorationResult.ConnectingToBaseNode -> onProgress(RecoveryState.ConnectingToBaseNode(resourceManager))
                is WalletRestorationResult.ConnectedToBaseNode -> onProgress(RecoveryState.ConnectedToBaseNode(resourceManager))
                is WalletRestorationResult.ScanningRoundFailed ->
                    onProgress(RecoveryState.ConnectionFailed(resourceManager, it.retryCount, it.retryLimit))
                is WalletRestorationResult.ConnectionToBaseNodeFailed -> {
                    if (it.retryCount == it.retryLimit) {
                        compositeDisposable.dispose()
                        compositeDisposable = CompositeDisposable()
                        tryNextBaseNode()
                    } else {
                        onProgress(RecoveryState.ConnectionFailed(resourceManager, it.retryCount, it.retryLimit))
                    }
                }
                is WalletRestorationResult.Progress -> onProgress(RecoveryState.Recovery(resourceManager, it.currentBlock, it.numberOfBlocks))
                is WalletRestorationResult.RecoveryFailed ->
                    onError(RestorationError.RecoveryInternalError(resourceManager, this@WalletRestoringFromSeedWordsViewModel::onErrorClosed))
                is WalletRestorationResult.Completed -> onSuccessRestoration()
            }
        }.addTo(compositeDisposable)
    }

    private fun onError(restorationError: RestorationError) {
        walletServiceLauncher.stopAndDelete()
        _errorDialog.postValue(restorationError.args)
    }

    private fun onSuccessRestoration() {
        sharedPrefsRepository.hasVerifiedSeedWords = true
        _navigation.postValue(WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted)
    }

    private fun onProgress(recoveryState: RecoveryState) = _recoveryState.postValue(recoveryState)

    private fun onErrorClosed() = _navigation.postValue(WalletRestoringFromSeedWordsNavigation.OnRestoreFailed)

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