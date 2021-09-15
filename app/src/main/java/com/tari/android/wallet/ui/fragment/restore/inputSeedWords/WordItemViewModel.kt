package com.tari.android.wallet.ui.fragment.restore.inputSeedWords

import androidx.lifecycle.MutableLiveData

class WordItemViewModel {

    val text = MutableLiveData("")

    var index = MutableLiveData(0)

    companion object {
        fun create(text: String): WordItemViewModel {
            return WordItemViewModel().apply {
                this.text.value = text
            }
        }
    }
}