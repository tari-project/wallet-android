package com.tari.android.wallet.ui.screen.settings.backup.writeDownSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel

class WriteDownSeedPhraseViewModel : CommonViewModel() {

    private val _seedWords = MutableLiveData<List<String>>()
    val seedWords: LiveData<List<String>> = _seedWords

    init {
        _seedWords.value = listOf()
        doOnWalletRunning { wallet ->
            try {
                _seedWords.postValue(wallet.getSeedWords())
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun showError() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.common_error_title),
            description = resourceManager.getString(R.string.back_up_seed_phrase_error),
        )
    }

    fun copySeedsToClipboard() {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
            clipText = seedWords.value!!.joinToString(" "),
        )
    }
}