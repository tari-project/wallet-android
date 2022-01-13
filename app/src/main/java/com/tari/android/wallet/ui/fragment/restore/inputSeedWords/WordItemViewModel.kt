package com.tari.android.wallet.ui.fragment.restore.inputSeedWords

import androidx.lifecycle.MutableLiveData

class WordItemViewModel private constructor(private val mnemonicList: List<String>) {

    val text = MutableLiveData("")

    var index = MutableLiveData(0)

    fun isValid() : Boolean = text.value.isNullOrEmpty() || mnemonicList.contains(text.value)

    companion object {
        fun create(text: String, mnemonicList: List<String>): WordItemViewModel {
            return WordItemViewModel(mnemonicList).apply {
                this.text.value = text
            }
        }
    }
}