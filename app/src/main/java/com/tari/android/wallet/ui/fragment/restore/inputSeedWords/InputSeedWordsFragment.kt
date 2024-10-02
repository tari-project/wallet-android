package com.tari.android.wallet.ui.fragment.restore.inputSeedWords

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentWalletInputSeedWordsBinding
import com.tari.android.wallet.extension.launchAndRepeatOnLifecycle
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.setTextSilently
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.showKeyboard
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.suggestions.SuggestionState
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.suggestions.SuggestionViewHolderItem
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.suggestions.SuggestionsAdapter
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar

class InputSeedWordsFragment : CommonFragment<FragmentWalletInputSeedWordsBinding, InputSeedWordsViewModel>() {

    private val suggestionsAdapter = SuggestionsAdapter()
    private var keyboardRegistrar: Unregistrar? = null

    override fun screenRecordingAlwaysDisable() = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentWalletInputSeedWordsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: InputSeedWordsViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        subscribeUI()

        viewModel.getFocusToNextElement(-1)
    }

    override fun onPause() {
        super.onPause()
        keyboardRegistrar?.unregister()
    }

    override fun onResume() {
        super.onResume()
        keyboardRegistrar = KeyboardVisibilityEvent.registerEventListener(requireActivity()) { viewModel.setSuggestionState(it) }
    }

    private fun setupUI() = with(ui) {
        seedWordsContainer.setOnThrottledClickListener { viewModel.getFocusToNextElement(-1) }
        continueCtaView.setOnThrottledClickListener {
            onFinishEntering()
            viewModel.startRestoringWallet()
        }
        seedWordsContainer.setOnLongClickListener {
            val word = seedWordsFlexboxLayout.getChildAt(viewModel.focusedIndex.value!!) as? WordTextView
            word?.ui?.text?.let {
                it.requestFocus()
                it.selectAll()
                it.performLongClick()
                it.performLongClick()
            }
            true
        }
        chooseBaseNodeButton.setVisible(DebugConfig.hardcodedBaseNodes)
        chooseCustomBaseNodeButton.setVisible(!DebugConfig.hardcodedBaseNodes)
        chooseBaseNodeButton.setOnClickListener { viewModel.navigation.postValue(Navigation.InputSeedWordsNavigation.ToBaseNodeSelection) }
        chooseCustomBaseNodeButton.setOnClickListener { viewModel.chooseCustomBaseNodeClick() }
        suggestionsAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.selectSuggestion(it) })
        suggestions.adapter = suggestionsAdapter
        suggestions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun subscribeUI() = with(viewModel) {
        observe(words) {
            if (ui.seedWordsFlexboxLayout.getChildAt(0) == null) {
                it.forEach { word -> addWord(word) }
            }
        }

        observe(addedWord) { addWord(it) }

        observe(removedWord) { removeWord(it) }

        observe(continueButtonState) { ui.continueCtaView.setState(it) }

        observe(finishEntering) { onFinishEntering() }

        observe(focusedIndex) { requestFocusAtIndex(it) }

        observe(suggestions) { showSuggestions(it) }

        observeOnLoad(isAllEntered)
        observeOnLoad(isInProgress)

        viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                customBaseNodeState.collect { state ->
                    ui.chooseCustomBaseNodeButton.text = getText(
                        if (state.isAddressSet) R.string.restore_from_seed_words_button_select_base_node_edit
                        else R.string.restore_from_seed_words_button_select_base_node_select
                    )
                }
            }
        }
    }

    private fun showSuggestions(suggestions: SuggestionState) = when (suggestions) {
        SuggestionState.Empty -> {
            setSuggestionsState(false)
            ui.suggestionsLabel.setText(R.string.restore_from_seed_words_autocompletion_no_suggestions)
        }

        SuggestionState.Hidden -> {
            ui.seedWordsSuggestions.setVisible(false)
        }

        SuggestionState.NotStarted -> {
            setSuggestionsState(false)
            ui.suggestionsLabel.setText(R.string.restore_from_seed_words_autocompletion_start_typing)
        }

        is SuggestionState.Suggested -> {
            suggestionsAdapter.update(suggestions.list.map { SuggestionViewHolderItem(it) }.toMutableList())
            setSuggestionsState(true)
        }
    }

    private fun setSuggestionsState(isSuggested: Boolean) {
        ui.seedWordsSuggestions.setVisible(true)
        ui.suggestionsContainer.setVisible(isSuggested)
        ui.suggestionsLabel.setVisible(!isSuggested)
    }

    private fun addWord(word: WordItemViewModel) {
        val wordView = WordTextView(requireContext()).apply {
            this@InputSeedWordsFragment.observe(word.text) {
                ui.text.setTextSilently(it)
            }
            this@InputSeedWordsFragment.observe(word.index) {
                val ime = if (it == SeedPhrase.SEED_PHRASE_LENGTH - 1)
                    EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
                ui.text.imeOptions = ime
            }
            setOnClickListener { viewModel.getFocus(word.index.value!!) }
            ui.removeView.setOnClickListener { viewModel.removeWord(word.index.value!!) }
            ui.text.doAfterTextChanged { viewModel.onCurrentWordChanges(word.index.value!!, it?.toString().orEmpty()) }
            ui.text.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && ui.text.text.isNullOrEmpty() && word.index.value!! != 0) {
                    viewModel.removeWord(word.index.value!!)
                    return@setOnKeyListener true
                }
                false
            }
            ui.text.setOnFocusChangeListener { _, hasFocus ->
                updateState(hasFocus, word.isValid())
                if (hasFocus) viewModel.getFocus(word.index.value!!, true)
            }
            updateState(false, word.isValid())
            ui.text.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    viewModel.finishEntering(word.index.value!!, ui.text.text?.toString().orEmpty())
                    return@setOnEditorActionListener true
                }
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.finishEntering()
                    return@setOnEditorActionListener true
                }
                false
            }
        }
        ui.seedWordsFlexboxLayout.addView(wordView, word.index.value!!)
    }

    private fun removeWord(word: WordItemViewModel) {
        ui.seedWordsFlexboxLayout.removeViewAt(word.index.value!!)
    }

    private fun requestFocusAtIndex(index: Int) {
        val textView = (ui.seedWordsFlexboxLayout.getChildAt(index) as WordTextView).ui.text
        textView.post {
            textView.clearFocus()
            textView.requestFocus()
            textView.setSelectionToEnd()
            requireActivity().showKeyboard()
        }
    }

    private fun onFinishEntering() {
        requireActivity().hideKeyboard()
        requireActivity().currentFocus?.clearFocus()
    }

    companion object {
        fun newInstance() = InputSeedWordsFragment()
    }
}