package com.tari.android.wallet.ui.screen.restore.inputSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.ffi.FFISeedWords
import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.debounce
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.component.loadingButton.LoadingButtonState
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.suggestions.SuggestionState
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.suggestions.SuggestionViewHolderItem
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.delay

class InputSeedWordsViewModel : CommonViewModel() {

    private var mnemonicList = mutableListOf<String>()

    private val _suggestions = MediatorLiveData<SuggestionState>()
    val suggestions: LiveData<SuggestionState> = _suggestions.debounce(50)

    private val _words = MutableLiveData<MutableList<WordItemViewModel>>()
    val words: LiveData<MutableList<WordItemViewModel>> = _words.debounce(50)

    private val _addedWord = SingleLiveEvent<WordItemViewModel>()
    val addedWord: LiveData<WordItemViewModel> = _addedWord

    private val _removedWord = SingleLiveEvent<WordItemViewModel>()
    val removedWord: LiveData<WordItemViewModel> = _removedWord

    private val _finishEntering = SingleLiveEvent<Unit>()
    val finishEntering: LiveData<Unit> = _finishEntering

    private val _focusedIndex = MutableLiveData<Int>()
    val focusedIndex: LiveData<Int> = _focusedIndex.debounce(50)

    private val _inProgress = MutableLiveData(false)
    val isInProgress: LiveData<Boolean> = _inProgress

    val isAllEntered: LiveData<Boolean> = _words.map { words ->
        words.all { it.text.value!!.isNotEmpty() } && words.size == SeedPhrase.SEED_PHRASE_LENGTH
    }

    private val _continueButtonState = MediatorLiveData<LoadingButtonState>()
    val continueButtonState: LiveData<LoadingButtonState> = _continueButtonState

    init {
        component.inject(this)

        _continueButtonState.addSource(isAllEntered) { updateContinueState() }
        _continueButtonState.addSource(isInProgress) { updateContinueState() }

        _words.value = mutableListOf()
        _focusedIndex.value = 0

        walletManager.deleteWallet()

        loadSuggestions()

        _suggestions.postValue(SuggestionState.Empty)

        _suggestions.addSource(focusedIndex) { processSuggestions() }
        _suggestions.addSource(_words) { processSuggestions() }
    }

    fun startRestoringWallet() {
        val words = _words.value!!.map { it.text.value!! }
        when (SeedPhrase.create(words)) {
            is SeedPhrase.SeedPhraseCreationResult.Success -> startRestoring(words)

            is SeedPhrase.SeedPhraseCreationResult.SeedPhraseNotCompleted -> onError(RestorationError.SeedPhraseTooShort(resourceManager))
            is SeedPhrase.SeedPhraseCreationResult.Failed -> onError(RestorationError.Unknown(resourceManager))
            is SeedPhrase.SeedPhraseCreationResult.InvalidSeedPhrase,
            is SeedPhrase.SeedPhraseCreationResult.InvalidSeedWord -> onError(RestorationError.Invalid(resourceManager))
        }
    }

    private fun loadSuggestions() {
        launchOnIo {
            val mnemonic = FFISeedWords.getMnemonicWordList(FFISeedWords.Language.English)
            val size = mnemonic.getLength()
            for (i in 0 until size) {
                mnemonicList.add(mnemonic.getAt(i))
            }
        }
    }

    private fun startRestoring(seedWords: List<String>) {
        _inProgress.postValue(true)

        launchOnIo {
            walletManager.doOnWalletRunning {
                tariNavigator.navigate(Navigation.InputSeedWords.ToRestoreFromSeeds)
                _inProgress.postValue(false)
            }
        }

        walletManager.start(seedWords)
    }

    private fun onError(restorationError: RestorationError) = showModularDialog(restorationError.args.getModular(resourceManager))

    fun addWord(index: Int, text: String = "") {
        val newWord = WordItemViewModel.create(text, mnemonicList)
        _words.value?.add(index, newWord)
        reindex()
        _addedWord.value = newWord
        _words.value = _words.value
        getFocus(index)
    }

    fun removeWord(index: Int) {
        val list = _words.value!!
        if (index !in list.indices) return
        val removedWord = list[index]
        val focusedIndex = _focusedIndex.value!!
        list.removeAt(index)
        reindex()
        _removedWord.value = removedWord
        _words.value = list
        if (focusedIndex != index) return
        if (list.isEmpty()) {
            addWord(0, "")
            getFocus(0)
        } else {
            when {
                focusedIndex == list.size -> getFocus(list.size - 1)
                index < focusedIndex -> getFocus((focusedIndex - 1).coerceAtLeast(0))
                else -> getFocus(focusedIndex)
            }
        }
    }

    fun onCurrentWordChanges(index: Int, text: String) {
        val list = _words.value!!
        if (!list.indices.contains(index)) return
        if (list[index].text.value!! == text) return

        val formattedText = text.split(" ", "\n", "\r").filter { it.isNotEmpty() }
        if (formattedText.isNotEmpty()) {
            list[index].text.value = formattedText[0]
            for ((formattedIndex, item) in formattedText.drop(1).withIndex()) {
                val nextIndex = index + formattedIndex + 1
                if (list.size < SeedPhrase.SEED_PHRASE_LENGTH) {
                    addWord(nextIndex, item)
                }
            }
            if (text.endsWith(" ")) {
                finishEntering(index, formattedText.last())
            } else {
                _words.value = list
            }
        } else {
            list[index].text.value = ""
            _words.value = list
        }
    }

    fun getFocusToNextElement(currentIndex: Int) {
        val list = _words.value!!
        if (list.isEmpty() || list.last().text.value!!.isNotEmpty() && list.size < SeedPhrase.SEED_PHRASE_LENGTH) {
            WordItemViewModel.create("", mnemonicList).apply {
                list.add(this)
                reindex()
                _addedWord.value = this
            }
            _words.value = list
        }

        val nextIndex = if (currentIndex == -1) list.size - 1 else (currentIndex + 1).coerceAtMost(list.size - 1)
        getFocus(nextIndex)
    }

    fun getFocus(index: Int, isSilent: Boolean = false) {
        if (!isSilent || _focusedIndex.value != index) {
            _focusedIndex.value = index
        }
        val list = _words.value!!
        if (index != list.size - 1 && list.last().text.value!!.isEmpty()) {
            removeWord(list.size - 1)
        }
    }

    fun finishEntering(index: Int, text: String) {
        val list = _words.value!!
        if (text.isNotEmpty() && list.size < SeedPhrase.SEED_PHRASE_LENGTH) {
            addWord(index + 1)
        }
        _words.value = _words.value
        getFocusToNextElement(index)
    }

    fun finishEntering() {
        _finishEntering.value = Unit
    }

    private fun reindex() {
        for ((index, word) in _words.value!!.withIndex()) {
            word.index.value = index
        }
    }

    private fun updateContinueState() {
        val title = resourceManager.getString(R.string.restore_from_seed_words_submit_title)
        val state = LoadingButtonState(title, isAllEntered.value!! && !_inProgress.value!!, _inProgress.value!!)
        _continueButtonState.postValue(state)
    }

    fun selectSuggestion(suggestionViewHolderItem: SuggestionViewHolderItem) {
        onCurrentWordChanges(_focusedIndex.value!!, suggestionViewHolderItem.suggestion + " ")
        if (_words.value.orEmpty().size == SeedPhrase.SEED_PHRASE_LENGTH) {
            launchOnIo {
                delay(100)
                launchOnMain {
                    finishEntering()
                }
            }
        }
    }

    fun setSuggestionState(isOpened: Boolean) {
        if (isOpened) {
            processSuggestions()
        } else {
            _suggestions.postValue(SuggestionState.Hidden)
        }
    }

    private fun processSuggestions() {
        val focusedIndex = _focusedIndex.value!!
        val words = _words.value.orEmpty()
        if (!words.indices.contains(focusedIndex)) return
        val focusedItem = _words.value.orEmpty()[focusedIndex]
        val text = focusedItem.text.value.orEmpty()
        val state = if (text.isEmpty()) {
            SuggestionState.NotStarted
        } else {
            val suggested = mnemonicList.filter { it.startsWith(text) }.toMutableList()
            if (suggested.isEmpty()) {
                SuggestionState.Empty
            } else {
                SuggestionState.Suggested(suggested)
            }
        }
        _suggestions.postValue(state)
    }

    sealed class RestorationError(title: String, message: String) {

        val args = SimpleDialogArgs(title = title, description = message)

        class Invalid(resourceManager: ResourceManager) : RestorationError(
            resourceManager.getString(R.string.common_error_title),
            resourceManager.getString(R.string.restore_from_seed_words_error_invalid_seed_word)
        )

        class SeedPhraseTooShort(resourceManager: ResourceManager) : RestorationError(
            resourceManager.getString(R.string.common_error_title),
            resourceManager.getString(R.string.restore_from_seed_words_error_phrase_too_short)
        )

        class Unknown(resourceManager: ResourceManager) : RestorationError(
            resourceManager.getString(R.string.common_error_title),
            resourceManager.getString(R.string.restore_from_seed_words_error_unknown)
        )
    }
}