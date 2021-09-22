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
package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_bg
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_invalid_bg
import com.tari.android.wallet.databinding.FragmentBaseNodeAddBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.BaseNodeAddressValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.Validator

/**
 * Base node configuration debug fragment.
 * If you reach this fragment with valid base node data in the clipboard in format
 * PUBLIC_KEY_HEX::ADDRESS then the fragment will split the clipboard data and paste the values
 * to input fields.
 *
 * @author The Tari Development Team
 */
internal class AddCustomBaseNodeFragment : CommonFragment<FragmentBaseNodeAddBinding, AddCustomBaseNodeViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBaseNodeAddBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel: AddCustomBaseNodeViewModel by viewModels()
        bindViewModel(viewModel)
        setupUI()
        observeUI()
    }

    override fun onStart() {
        super.onStart()
        checkClipboardForValidBaseNodeData()
    }

    override fun onDestroy() {
        requireActivity().hideKeyboard()
        super.onDestroy()
    }

    private fun setupUI() = with(ui) {
        backCtaView.setOnThrottledClickListener { requireActivity().onBackPressed() }
        saveButton.setOnThrottledClickListener { viewModel.saveCustomNode() }
        nameEditText.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.onNameChanged(text?.toString().orEmpty()) })
        publicKeyHexEditText.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.onPublicKeyHexChanged(text?.toString().orEmpty()) })
        addressEditText.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.onAddressChanged(text?.toString().orEmpty()) })

        publicKeyHexEditText.requestFocus()
        requireActivity().showKeyboard()
    }

    private fun observeUI() = with(viewModel) {

        observe(publicKeyHexValidation) {
            ui.publicKeyHexEditText.background = getValidBg(it == Validator.State.Invalid)
            ui.invalidPublicKeyHexTextView.setVisible(it == Validator.State.Invalid, View.INVISIBLE)
        }

        observe(addressValidationState) {
            ui.addressEditText.background = getValidBg(it == Validator.State.Invalid)
            ui.invalidAddressTextView.setVisible(it == Validator.State.Invalid, View.INVISIBLE)
        }

        observeOnLoad(nameText)
        observeOnLoad(onionAddressText)
        observeOnLoad(publicHexText)
    }

    private fun getValidBg(isInvalid: Boolean) = drawable(if (isInvalid) base_node_config_edit_text_invalid_bg else base_node_config_edit_text_bg)

    /**
     * Checks whether a the public key and address are in the clipboard in the expected format.
     */
    private fun checkClipboardForValidBaseNodeData() {
        val clipboardManager = (activity?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        // if clipboard contains at least 1 emoji, then display paste emoji banner
        if (BaseNodeAddressValidator().validate(clipboardString) == Validator.State.Valid) {
            val input = clipboardString.split("::")
            ui.publicKeyHexEditText.setText(input[0])
            ui.addressEditText.setText(input[1])
        }
    }

    companion object {
        fun getNewInstance() = AddCustomBaseNodeFragment()
    }
}