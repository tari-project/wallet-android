package com.tari.android.wallet.ui.screen.settings.backup.writeDownSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.util.extension.getWithError
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs

class WriteDownSeedPhraseViewModel : CommonViewModel() {

    private val _seedWords = MutableLiveData<List<String>>()
    val seedWords: LiveData<List<String>> = _seedWords

    init {
        _seedWords.value = listOf()
        doOnWalletServiceConnected { getSeedWords() }
    }

    private fun getSeedWords() {
        val seedWords = walletService.getWithError({ if (it != WalletError.NoError) showError() }) { error, wallet -> wallet.getSeedWords(error) }
        if (seedWords != null) {
            _seedWords.postValue(seedWords)
        } else {
            showError()
        }
    }

    private fun showError() {
        showModularDialog(
            SimpleDialogArgs(
                title = resourceManager.getString(R.string.common_error_title),
                description = resourceManager.getString(R.string.back_up_seed_phrase_error),
            ).getModular(resourceManager)
        )
    }

    fun copySeedsToClipboard() {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
            clipText = seedWords.value!!.joinToString(" "),
        )
    }
}