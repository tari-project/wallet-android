package com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel

class VerifySeedPhraseViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    private val _nothingSelected = MutableLiveData(false)
    val nothingSelected: LiveData<Boolean> = _nothingSelected

    private val _selectionIsCompleted = MutableLiveData(false)
    val selectionIsCompleted: LiveData<Boolean> = _selectionIsCompleted

    private val _addWord = MutableLiveData<Pair<String, Int>>()
    val addWord: LiveData<Pair<String, Int>> = _addWord

    lateinit var sortedPhrase: SeedPhrase
    lateinit var selectionPhrase: SelectionSequence

    fun initWithSeedPhrase(seedWords: ArrayList<String>) {
        val seedPhrase = SeedPhrase(seedWords)
        val (sorted, selectionSequence) = seedPhrase.startSelection()
        this.sortedPhrase = sorted
        this.selectionPhrase = selectionSequence

        evaluateEnteredPhrase()
    }

    fun selectWord(index: Int) {
        selectionPhrase.add(index)
        _addWord.postValue(Pair(sortedPhrase[index], index))
        evaluateEnteredPhrase()
    }

    fun unselectWord(index: Int) {
        selectionPhrase.remove(index)
        evaluateEnteredPhrase()
    }

    private fun evaluateEnteredPhrase() {
        _nothingSelected.postValue(selectionPhrase.isEmpty)
        _selectionIsCompleted.postValue(selectionPhrase.isComplete)
    }

    fun verify() {
        tariSettingsSharedRepository.hasVerifiedSeedWords = true
        tariNavigator.navigate(Navigation.AllSettings.BackToBackupSettings)
    }
}