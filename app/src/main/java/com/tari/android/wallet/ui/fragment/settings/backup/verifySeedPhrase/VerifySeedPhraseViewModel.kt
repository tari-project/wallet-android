package com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import javax.inject.Inject

class VerifySeedPhraseViewModel() : CommonViewModel() {

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    init {
        component.inject(this)
    }

    private val _navigation = SingleLiveEvent<VerifySeedPhraseNavigation>()
    val navigation: LiveData<VerifySeedPhraseNavigation> = _navigation

    private val _nothingSelected = MutableLiveData(false)
    val nothingSelected: LiveData<Boolean> = _nothingSelected

    private val _selectionIsCompleted = MutableLiveData(false)
    val selectionIsCompleted: LiveData<Boolean> = _selectionIsCompleted

    private val _addWord = MutableLiveData<Pair<String, Int>>()
    val addWord: LiveData<Pair<String, Int>> = _addWord

    lateinit var shuffledPhrase: SeedPhrase
    lateinit var selectionPhrase: SelectionSequence

    fun initWithSeedPhrase(seedWords: ArrayList<String>) {
        val seedPhrase = SeedPhrase(seedWords)
        val (shuffled, selectionSequence) = seedPhrase.startSelection()
        this.shuffledPhrase = shuffled
        this.selectionPhrase = selectionSequence

        evaluateEnteredPhrase()
    }

    fun selectWord(index: Int) {
        selectionPhrase.add(index)
        _addWord.postValue(Pair(shuffledPhrase[index], index))
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
        _navigation.postValue(VerifySeedPhraseNavigation.ToSeedPhraseVerificationComplete)
    }
}