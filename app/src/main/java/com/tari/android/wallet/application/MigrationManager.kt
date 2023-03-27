package com.tari.android.wallet.application

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.service.WalletService
import com.tari.android.wallet.ui.common.SimpleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor() {

    private val minValidVersion = DefaultArtifactVersion("0.49.0")

    private val simpleViewModel = SimpleViewModel()

    fun validateVersion(onValid: () -> Unit, onError: () -> Unit) {
        simpleViewModel.doOnConnectedToWallet {
            val walletVersion = getCurrentWalletVersion()

            if (walletVersion.isEmpty() || DefaultArtifactVersion(walletVersion) < minValidVersion) {
                simpleViewModel.viewModelScope.launch(Dispatchers.Main) { onError() }
            } else {
                simpleViewModel.viewModelScope.launch(Dispatchers.Main) { onValid() }
            }
        }
    }

    fun updateWalletVersion() {
        simpleViewModel.doOnConnectedToWallet {
            FFIWallet.instance?.setKeyValue(WalletService.Companion.KeyValueStorageKeys.version, minValidVersion.toString())
        }
    }

    private fun getCurrentWalletVersion(): String = try {
        FFIWallet.instance?.getKeyValue(WalletService.Companion.KeyValueStorageKeys.version).orEmpty()
    } catch (e: Throwable) {
        if (e is FFIException && e.error?.code == WalletError.ValuesNotFound.code) "" else throw e
    }
}