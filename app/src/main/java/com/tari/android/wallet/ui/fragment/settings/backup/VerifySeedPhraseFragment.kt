/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.settings.backup

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.databinding.FragmentVerifySeedPhraseBinding
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.SharedPrefsWrapper
import java.util.*
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.collections.ArrayList

class VerifySeedPhraseFragment @Deprecated(
    """Use newInstance() and supply all
the necessary data via arguments instead, as fragment's default no-op constructor is used by the
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    private lateinit var ui: FragmentVerifySeedPhraseBinding
    private val state by lazy {
        val words = requireArguments().getStringArrayList(SEED_WORDS_KEY)!!
        ViewModelProvider(this, VerificationStateFactory(words)).get(VerificationState::class.java)
    }
    private val avenirHeavy: Typeface
            by lazy(NONE) { CustomFont.AVENIR_LT_STD_BLACK.asTypeface(requireContext()) }
    private val selectedWordThemedContext: Context
            by lazy(NONE) { ContextThemeWrapper(requireContext(), R.style.SelectedSeedPhraseWord) }
    private val selectedWordEndMargin: Int
            by lazy(NONE) { dimenPx(verify_seed_phrase_selected_word_end_margin) }
    private val wordBottomMargin: Int
            by lazy(NONE) { dimenPx(verify_seed_phrase_word_bottom_margin) }
    private val selectableWordHorizontalPadding: Int
            by lazy(NONE) { dimenPx(verify_seed_phrase_selectable_word_horizontal_padding) }
    private val selectableWordVerticalPadding: Int
            by lazy(NONE) { dimenPx(verify_seed_phrase_selectable_word_vertical_padding) }
    private val selectableWordEndMargin: Int
            by lazy(NONE) { dimenPx(verify_seed_phrase_selectable_word_end_margin) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentVerifySeedPhraseBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        fillSelectableWordsContainer()
        fillSelectedWordsContainer()
        evaluateEnteredPhrase()
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.continueCtaView.setOnClickListener(ThrottleClick {
            it.animateClick {
                sharedPrefs.hasVerifiedSeedWords = true
                (requireActivity() as BackupSettingsRouter)
                    .onSeedPhraseVerificationComplete(this)
            }
        })
    }

    private fun fillSelectableWordsContainer() {
        val selectableWordThemedContext =
            ContextThemeWrapper(requireContext(), R.style.SelectableSeedPhraseWord)
        state.shuffled.withIndex().forEach { iv ->
            TextView(selectableWordThemedContext).apply {
                typeface = avenirHeavy
                text = iv.value
                layoutParams = FlexboxLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    .apply { setMargins(0, 0, selectableWordEndMargin, wordBottomMargin) }
                setPadding(
                    selectableWordHorizontalPadding,
                    selectableWordVerticalPadding,
                    selectableWordHorizontalPadding,
                    selectableWordVerticalPadding
                )
                val selected = state.selection.contains(iv.index)
                visibility = if (selected) INVISIBLE else VISIBLE
                isEnabled = !selected
                setOnClickListener { view ->
                    view.isEnabled = false
                    ValueAnimator.ofFloat(1F, 0F).apply {
                        addUpdateListener { view.alpha = it.animatedValue as Float }
                        addListener(onEnd = {
                            state.selection.add(iv.index)
                            view.visibility = INVISIBLE
                            addSelectedWord(state.shuffled[iv.index], iv.index)
                            ui.selectWordsLabelView.gone()
                            evaluateEnteredPhrase()
                        })
                    }.start()
                }
                ui.selectableWordsFlexboxLayout.addView(this)
            }
        }
    }

    private fun fillSelectedWordsContainer() {
        state.selection.currentSelection.forEach {
            addSelectedWord(it.second, it.first)
        }
    }

    private fun addSelectedWord(word: String, index: Int) {
        TextView(selectedWordThemedContext).apply {
            typeface = avenirHeavy
            text = word
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            layoutParams = FlexboxLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                .apply { setMargins(0, 0, selectedWordEndMargin, wordBottomMargin) }
            setOnClickListener { view ->
                updateVerifyButtonState(enable = false)
                view.isEnabled = false
                val selectableWordView = ui.selectableWordsFlexboxLayout[index]
                selectableWordView.visibility = VISIBLE
                ValueAnimator.ofFloat(1F, 0F).apply {
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        view.alpha = value
                        selectableWordView.alpha = 1F - value
                    }
                    addListener(
                        onStart = {
                            ui.sequenceCorrectLabelView.gone()
                            ui.sequenceInvalidLabelView.gone()
                            ui.selectableWordsFlexboxLayout.visible()
                        },
                        onEnd = {
                            state.selection.remove(index)
                            ui.selectedWordsFlexboxLayout.removeView(view)
                            selectableWordView.isEnabled = true
                            evaluateEnteredPhrase()
                        })
                }.start()
            }
            ui.selectedWordsFlexboxLayout.addView(this)
        }
    }

    private fun updateLabelVisibility() {
        ui.selectWordsLabelView.visibility = if (state.selection.isEmpty) VISIBLE else GONE
    }

    private fun evaluateEnteredPhrase() {
        updateLabelVisibility()
        if (state.selection.isComplete) {
            ui.selectableWordsFlexboxLayout.gone()
            val matches = state.selection.matchesOriginalPhrase()
            updateVerifyButtonState(matches)
            ui.sequenceCorrectLabelView.visibility = if (matches) VISIBLE else GONE
            ui.sequenceInvalidLabelView.visibility = if (matches) GONE else VISIBLE
        } else {
            ui.selectableWordsFlexboxLayout.visible()
            ui.sequenceInvalidLabelView.gone()
            ui.sequenceCorrectLabelView.gone()
            updateVerifyButtonState(false)
        }
    }

    private fun updateVerifyButtonState(enable: Boolean) {
        if (ui.continueCtaView.isEnabled == enable) return
        ui.continueCtaView.isEnabled = enable
        ui.continueCtaView.setTextColor(
            if (enable) Color.WHITE
            else color(R.color.seed_phrase_button_disabled_text_color)
        )
    }

    internal class VerificationStateFactory(private val words: List<String>) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            VerificationState(SeedPhrase(words)) as T
    }

    internal class VerificationState(seedPhrase: SeedPhrase) : ViewModel() {

        val shuffled: SeedPhrase
        val selection: SelectionSequence

        init {
            val (shuffled, selectionSequence) = seedPhrase.startSelection()
            this.shuffled = shuffled
            this.selection = selectionSequence
        }

    }

    internal class SeedPhrase(private val seedWords: List<String>) : Iterable<String> {
        val length
            get() = seedWords.size

        private fun shuffled(): SeedPhrase = SeedPhrase(seedWords.shuffled())

        fun consistsOf(result: List<String>): Boolean = seedWords == result

        operator fun get(index: Int): String {
            if (index >= length) throw IllegalArgumentException(
                "Selection index ($index) isn't less than original phrase's length " +
                        "($length)\nPhrase: $this"
            )
            return seedWords[index]
        }

        fun startSelection() = shuffled().let { s -> Pair(s, SelectionSequence(this, s)) }

        override fun iterator(): Iterator<String> = seedWords.iterator()

        override fun equals(other: Any?): Boolean =
            this === other || javaClass == other?.javaClass && seedWords == (other as SeedPhrase).seedWords

        override fun hashCode(): Int = seedWords.hashCode()
        override fun toString(): String = "Phrase(words=${seedWords.joinToString()})"

    }

    internal class SelectionSequence(
        private val original: SeedPhrase,
        private val shuffled: SeedPhrase
    ) {
        private val selections = LinkedList<Int>()
        val size: Int
            get() = selections.size
        val currentSelection: List<Pair<Int, String>>
            get() = selections.map { Pair(it, shuffled[it]) }
        val isEmpty: Boolean
            get() = selections.isEmpty()
        val isComplete: Boolean
            get() = selections.size == shuffled.length

        init {
            if (original.length != shuffled.length) {
                throw IllegalArgumentException(
                    "Original and shuffled phrases' lengths aren't equal." +
                            "\nOriginal: $original\nShuffled: $shuffled"
                )
            }
        }

        fun add(index: Int) {
            if (size == original.length) throw IllegalArgumentException(
                "Selection sequence does already have the necessary size" +
                        "\nTried to add: (index=$index, phrase=$original,shuffled=$shuffled)"
            )
            if (index < 0 || index >= original.length)
                throw IllegalArgumentException(
                    "Selection index ($index) is invalid (either negative or bigger than phrase " +
                            "length)\nPhrase: ${original.length}"
                )
            if (selections.indexOfFirst { it == index } != -1)
                throw IllegalArgumentException("This selection ($index) is already present: $this")
            selections.add(index)
        }

        fun remove(index: Int) {
            selections
                .indexOfFirst { it == index }
                .takeIf { it != -1 }
                ?.also { selections.removeAt(it) }
                ?: throw IllegalArgumentException("Index $index is not added. $this")
        }

        fun matchesOriginalPhrase(): Boolean {
            val result = currentSelection
            if (result.size != original.length) throw IllegalStateException(
                "Current selection length (${result.size}) does not match target length " +
                        "(${original.length}).\nCurrent sequence: ${result.joinToString()}" +
                        "\nPhrase to be matched against: $original"
            )
            return original.consistsOf(result.map(Pair<Int, String>::second))
        }

        fun contains(index: Int): Boolean = selections.contains(index)

        override fun toString(): String =
            "SelectionSequence(original=$original, shuffled=$shuffled, selections=$selections)"

    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance(words: List<String>): VerifySeedPhraseFragment =
            VerifySeedPhraseFragment().also {
                it.arguments =
                    Bundle().apply { putStringArrayList(SEED_WORDS_KEY, ArrayList(words)) }
            }

        private const val SEED_WORDS_KEY = "wordz"
    }

}
