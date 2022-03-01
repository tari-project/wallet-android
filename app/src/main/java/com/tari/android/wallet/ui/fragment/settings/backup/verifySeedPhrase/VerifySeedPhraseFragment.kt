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
package com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.core.view.get
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentVerifySeedPhraseBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*

class VerifySeedPhraseFragment : CommonFragment<FragmentVerifySeedPhraseBinding, VerifySeedPhraseViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentVerifySeedPhraseBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: VerifySeedPhraseViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.initWithSeedPhrase(requireArguments().getStringArrayList(SEED_WORDS_KEY)!!)

        setupUI()

        observeUI()
    }

    private fun setupUI() {
        fillSelectableWordsContainer()
        fillSelectedWordsContainer()
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.continueCtaView.setOnClickListener(ThrottleClick { it.animateClick { viewModel.verify() } })
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(nothingSelected) { ui.selectWordsLabelView.setVisible(it) }

        observe(selectionIsCompleted) { onEndSelection(it) }

        observe(addWord) { addSelectedWord(it.first, it.second) }
    }

    private fun processNavigation(navigation: VerifySeedPhraseNavigation) {
        val router = requireActivity() as BackupSettingsRouter
        when (navigation) {
            VerifySeedPhraseNavigation.ToSeedPhraseVerificationComplete -> router.onSeedPhraseVerificationComplete(this)
        }
    }

    private fun fillSelectableWordsContainer() {
        viewModel.sortedPhrase.withIndex().forEach { iv ->
            SelectableWordTextView.createSelectableWord(requireContext(), false).apply {
                text = iv.value
                val selected = viewModel.selectionPhrase.contains(iv.index)
                visibility = if (selected) INVISIBLE else VISIBLE
                isEnabled = !selected
                setOnClickListener { view ->
                    view.isEnabled = false
                    ValueAnimator.ofFloat(1F, 0F).apply {
                        addUpdateListener { view.alpha = it.animatedValue as Float }
                        addListener(onEnd = {
                            view.visibility = INVISIBLE
                            ui.selectWordsLabelView.gone()
                            viewModel.selectWord(iv.index)
                        })
                    }.start()
                }
                ui.selectableWordsFlexboxLayout.addView(this)
            }
        }
    }

    private fun fillSelectedWordsContainer() {
        viewModel.selectionPhrase.currentSelection.forEach {
            addSelectedWord(it.second, it.first)
        }
    }

    private fun addSelectedWord(word: String, index: Int) {
        SelectableWordTextView.createSelectableWord(requireContext(), true).apply {
            text = word
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
                            ui.selectableWordsFlexboxLayout.removeView(view)
                            selectableWordView.isEnabled = true
                            viewModel.unselectWord(index)
                        })
                }.start()
            }
            ui.selectedWordsFlexboxLayout.addView(this)
        }
    }

    private fun onEndSelection(isEnd: Boolean) {
        ui.selectableWordsFlexboxLayout.setVisible(!isEnd)
        if (isEnd) {
            val matches = viewModel.selectionPhrase.matchesOriginalPhrase()
            ui.sequenceCorrectLabelView.setVisible(matches)
            ui.sequenceInvalidLabelView.setVisible(!matches)
            updateVerifyButtonState(matches)
        } else {
            ui.sequenceInvalidLabelView.gone()
            ui.sequenceCorrectLabelView.gone()
            updateVerifyButtonState(false)
        }
    }

    private fun updateVerifyButtonState(enable: Boolean) = with(ui) {
        continueCtaView.isEnabled = enable
    }

    companion object {
        fun newInstance(words: List<String>): VerifySeedPhraseFragment =
            VerifySeedPhraseFragment().also {
                it.arguments = Bundle().apply { putStringArrayList(SEED_WORDS_KEY, ArrayList(words)) }
            }

        private const val SEED_WORDS_KEY = "wordz"
    }
}

