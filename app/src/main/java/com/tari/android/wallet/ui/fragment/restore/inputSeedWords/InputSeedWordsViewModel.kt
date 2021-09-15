package com.tari.android.wallet.ui.fragment.restore.inputSeedWords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.component.loadingButton.LoadingButtonState
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

internal class InputSeedWordsViewModel() : CommonViewModel() {

    @Inject
    lateinit var seedPhraseRepository: SeedPhraseRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    private val _navigation = SingleLiveEvent<InputSeedWordsNavigation>()
    val navigation: LiveData<InputSeedWordsNavigation> = _navigation

    private val _words = MutableLiveData<MutableList<WordItemViewModel>>()
    val words: LiveData<MutableList<WordItemViewModel>> = _words

    private val _addedWord = SingleLiveEvent<WordItemViewModel>()
    val addedWord: LiveData<WordItemViewModel> = _addedWord

    private val _removedWord = SingleLiveEvent<WordItemViewModel>()
    val removedWord: LiveData<WordItemViewModel> = _removedWord

    private val _finishEntering = SingleLiveEvent<Unit>()
    val finishEntering: LiveData<Unit> = _finishEntering

    private val _focusedIndex = MutableLiveData<Int>()
    val focusedIndex: LiveData<Int> = _focusedIndex

    private val _inProgress = MutableLiveData(false)
    val isInProgress: LiveData<Boolean> = _inProgress

    val isAllEntered: LiveData<Boolean> = Transformations.map(_words) { words ->
        words.all { it.text.value!!.isNotEmpty() } && words.size == SeedPhrase.SeedPhraseLength
    }

    private val _continueButtonState = MediatorLiveData<LoadingButtonState>()
    val continueButtonState: LiveData<LoadingButtonState> = _continueButtonState

    init {
        component?.inject(this)

        _continueButtonState.addSource(isAllEntered) { updateContinueState() }
        _continueButtonState.addSource(isInProgress) { updateContinueState() }

        _words.value = mutableListOf()
        _focusedIndex.value = 0
        clear()
    }

    fun startRestoringWallet() {
        val words = _words.value!!.map { it.text.value!! }
        val seedPhrase = SeedPhrase()
        val result = seedPhrase.init(words)

        if (result == SeedPhrase.SeedPhraseCreationResult.Success) {
            seedPhraseRepository.save(seedPhrase)
            startRestoring()
        } else {
            handleSeedPhraseResult(result)
        }
    }

    private fun startRestoring() {
        _inProgress.postValue(true)
        EventBus.walletState.publishSubject.distinct().subscribe {
            when (it) {
                is WalletState.Failed -> {
                    onError(RestorationError.Unknown(resourceManager))
                    _inProgress.postValue(false)
                    clear()
                }
                WalletState.Running -> {
                    _navigation.postValue(InputSeedWordsNavigation.ToRestoreFormSeedWordsInProgress)
                    _inProgress.postValue(false)
                }
                else -> Unit
            }
        }.addTo(compositeDisposable)

        walletServiceLauncher.start()
    }

    private fun handleSeedPhraseResult(result: SeedPhrase.SeedPhraseCreationResult) {
        val errorDialogArgs = when (result) {
            is SeedPhrase.SeedPhraseCreationResult.Failed -> RestorationError.Unknown(resourceManager)
            SeedPhrase.SeedPhraseCreationResult.InvalidSeedPhrase,
            SeedPhrase.SeedPhraseCreationResult.InvalidSeedWord -> RestorationError.Invalid(resourceManager)
            SeedPhrase.SeedPhraseCreationResult.SeedPhraseNotCompleted -> RestorationError.SeedPhraseTooShort(resourceManager)
            else -> RestorationError.Unknown(resourceManager)
        }
        onError(errorDialogArgs)
    }

    private fun onError(restorationError: RestorationError) = _errorDialog.postValue(restorationError.args)

    private fun clear() {
        walletServiceLauncher.stopAndDelete()
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    fun addWord(index: Int, text: String = "") {
        val newWord = WordItemViewModel.create(text)
        _words.value?.add(index, newWord)
        reindex()
        _addedWord.value = newWord
        _words.value = _words.value
        getFocus(index)
    }

    fun removeWord(index: Int) {
        val list = _words.value!!
        val removedWord = list[index]
        list.removeAt(index)
        reindex()
        _removedWord.value = removedWord
        _words.value = list
        if (list.isEmpty()) {
            addWord(0, "")
            getFocus(0)
        } else {
            val focusedIndex = _focusedIndex.value!!
            when {
                focusedIndex == list.size -> getFocus(list.size - 1)
                index < focusedIndex -> getFocus((focusedIndex - 1).coerceAtLeast(0))
                else -> getFocus(focusedIndex)
            }
        }
    }

    fun onCurrentWordChanges(index: Int, text: String) {
        val list = _words.value!!
        if (list[index].text.value!! == text) return

        val formattedText = text.split(" ", "\n", "\r").filter { it.isNotEmpty() }
        if (formattedText.isNotEmpty()) {
            list[index].text.value = formattedText[0]
            for ((formattedIndex, item) in formattedText.drop(1).withIndex()) {
                val nextIndex = index + formattedIndex + 1
                if (list.size < SeedPhrase.SeedPhraseLength) {
                    addWord(nextIndex, item)
                }
            }
            if (text.endsWith(" ")) {
                finishEntering(index, formattedText.last())
            } else {
                _words.value = list
            }
        }
    }

    fun getFocusToNextElement(currentIndex: Int) {
        val list = _words.value!!
        if (list.isEmpty() ||
            list.isNotEmpty() && list.last().text.value!!.isNotEmpty() && list.size < SeedPhrase.SeedPhraseLength
        ) {
            WordItemViewModel().apply {
                list.add(this)
                reindex()
                _addedWord.value = this
            }
            _words.value = list
        }

        val nextIndex = if (currentIndex == -1) list.size - 1 else Math.min(currentIndex + 1, list.size - 1)
        getFocus(nextIndex)
    }

    fun getFocus(index: Int, isSilent: Boolean = false) {
        if (!isSilent || isSilent && _focusedIndex.value != index) {
            _focusedIndex.value = index
        }
        val list = _words.value!!
        if (index != list.size - 1 && list.last().text.value!!.isEmpty()) {
            removeWord(list.size - 1)
        }
    }

    fun finishEntering(index: Int, text: String) {
        val list = _words.value!!
        if (text.isNotEmpty() && list.size < SeedPhrase.SeedPhraseLength) {
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

    sealed class RestorationError(title: String, message: String) {

        val args = ErrorDialogArgs(title, message)

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