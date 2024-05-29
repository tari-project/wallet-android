package com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs

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
            ErrorDialogArgs(
                title = resourceManager.getString(R.string.common_error_title),
                description = resourceManager.getString(R.string.back_up_seed_phrase_error),
            ).getModular(resourceManager)
        )
    }
}