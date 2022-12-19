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
package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.enter_backup_password_page_desc_general_part
import com.tari.android.wallet.R.string.enter_backup_password_page_desc_highlighted_part
import com.tari.android.wallet.databinding.FragmentEnterRestorePasswordBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.restore.activity.WalletRestoreRouter

class EnterRestorationPasswordFragment : CommonFragment<FragmentEnterRestorePasswordBinding, EnterRestorationPasswordViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentEnterRestorePasswordBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: EnterRestorationPasswordViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        observeUI()
    }

    private fun setupUI() = with(ui) {
        setPageDescription()
        passwordEditText.requestFocus()
        requireActivity().showKeyboard()
        restoringProgressBar.setWhite()
        toolbar.backPressedAction = viewModel::onBack
        restoreWalletCtaView.setOnThrottledClickListener { viewModel.onRestore(passwordEditText.text?.toString().orEmpty()) }
        passwordEditText.addTextChangedListener(afterTextChanged = this@EnterRestorationPasswordFragment::afterTextChanged)
    }

    private fun observeUI() = with(viewModel) {
        observe(state) { processState(it) }

        observe(navigation) { processNavigation(it) }
    }

    private fun processState(state: EnterRestorationPasswordState) {
        when (state) {
            EnterRestorationPasswordState.InitState -> showInputUI()
            EnterRestorationPasswordState.RestoringInProgressState -> showRestoringUI()
            EnterRestorationPasswordState.WrongPasswordErrorState -> {
                showWrongPasswordErrorLabels()
                showInputUI()
            }
        }
    }

    private fun processNavigation(navigation: EnterRestorationPasswordNavigation) {
        val router = requireActivity() as WalletRestoreRouter
        when (navigation) {
            EnterRestorationPasswordNavigation.OnRestore -> router.onRestoreCompleted()
        }
    }

    private fun afterTextChanged(editable: Editable?) = with(ui) {
        setRestoreWalletCTAState((editable?.length ?: 0) != 0)
        wrongPasswordLabelView.gone()
    }

    private fun setRestoreWalletCTAState(isEnabled: Boolean) = with(ui) {
        if (restoreWalletCtaView.isEnabled == isEnabled) return
        restoreWalletCtaView.isEnabled = isEnabled
        restoreWalletTextView.setTextColor(
            if (isEnabled) Color.WHITE
            else color(seed_phrase_button_disabled_text_color)
        )
    }

    private fun showRestoringUI() = with(ui) {
        passwordEditText.isEnabled = false
        restoreWalletCtaView.isClickable = false
        restoreWalletTextView.gone()
        restoringProgressBar.visible()
        requireActivity().hideKeyboard()
    }

    private fun showInputUI() = with(ui) {
        passwordEditText.isEnabled = true
        restoreWalletCtaView.isClickable = true
        restoreWalletTextView.visible()
        restoringProgressBar.gone()
        passwordEditText.requestFocus()
        requireActivity().showKeyboard()
    }

    private fun showWrongPasswordErrorLabels() = with(ui) {
        enterPasswordLabelTextView.setTextColor(color(common_error))
        passwordEditText.setTextColor(color(common_error))
        wrongPasswordLabelView.visible()
    }

    private fun setPageDescription() {
        val generalPart = string(enter_backup_password_page_desc_general_part)
        val highlightedPart = SpannableString(string(enter_backup_password_page_desc_highlighted_part))
        val spanColor = ForegroundColorSpan(color(black))
        highlightedPart.setSpan(spanColor, 0, highlightedPart.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        ui.pageDescriptionTextView.text = SpannableStringBuilder().apply {
            insert(0, generalPart)
            insert(generalPart.length, " ")
            insert(generalPart.length + 1, highlightedPart)
            insert(generalPart.length + highlightedPart.length + 1, ".")
        }
    }

    companion object {
        fun newInstance() = EnterRestorationPasswordFragment()
    }
}