package com.tari.android.wallet.application

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.service.WalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class MigrationManager : CommonViewModel() {

    private val minValidVersion = DefaultArtifactVersion("0.44.0")

    fun validateVersion(onValid: () -> Unit, onError: () -> Unit) {
        doOnConnectedToWallet {
            val walletVersion = getCurrentWalletVersion()

            if (walletVersion.isEmpty() || DefaultArtifactVersion(walletVersion) < minValidVersion) {
                viewModelScope.launch(Dispatchers.Main) { onError() }
            } else {
                viewModelScope.launch(Dispatchers.Main) { onValid() }
            }
        }
    }

    fun updateWalletVersion() {
        doOnConnectedToWallet {
            FFIWallet.instance?.setKeyValue(WalletService.Companion.KeyValueStorageKeys.version, minValidVersion.toString())
        }
    }

    private fun getCurrentWalletVersion(): String = try {
        FFIWallet.instance?.getKeyValue(WalletService.Companion.KeyValueStorageKeys.version).orEmpty()
    } catch (e: Throwable) {
        if (e is FFIException && e.error?.code == WalletError.ValuesNotFound.code) "" else throw e
    }
}