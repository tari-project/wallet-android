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
package com.tari.android.wallet.ui.fragment.debug

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_bg
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_invalid_bg
import com.tari.android.wallet.databinding.FragmentBaseNodeConfigBinding
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import javax.inject.Inject

/**
 * Base node configuration debug fragment.
 * If you reach this fragment with valid base node data in the clipboard in format
 * PUBLIC_KEY_HEX::ADDRESS then the fragment will split the clipboard data and paste the values
 * to input fields.
 *
 * @author The Tari Development Team
 */
internal class BaseNodeConfigFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private val clipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")
    private val publicKeyRegex = Regex("[a-zA-Z0-9]{64}")
    private val addressRegex = Regex("/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")

    private lateinit var ui: FragmentBaseNodeConfigBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentBaseNodeConfigBinding.inflate(inflater, container, false)
            .also { ui = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        setupUi()
    }

    override fun onStart() {
        super.onStart()
        checkClipboardForValidInput()
    }

    private fun setupUi() {
        UiUtil.setProgressBarColor(ui.progressBar, color(white))
        ui.apply {
            publicKeyHexTextView.text = sharedPrefsWrapper.baseNodePublicKeyHex
            addressTextView.text = sharedPrefsWrapper.baseNodeAddress
            progressBar.gone()
            invalidPublicKeyHexTextView.invisible()
            invalidAddressTextView.invisible()
            saveButton.setOnClickListener { saveButtonClicked(it) }
            publicKeyHexEditText.addTextChangedListener(
                onTextChanged = { _, _, _, _ -> onPublicKeyHexChanged() }
            )
            addressEditText.addTextChangedListener(
                onTextChanged = { _, _, _, _ -> onAddressChanged() }
            )
        }
    }

    private fun onPublicKeyHexChanged() {
        ui.publicKeyHexEditText.background = drawable(base_node_config_edit_text_bg)
        ui.invalidPublicKeyHexTextView.invisible()
    }

    private fun onAddressChanged() {
        ui.addressEditText.background = drawable(base_node_config_edit_text_bg)
        ui.invalidAddressTextView.invisible()
    }

    /**
     * Checks whether a the public key and address are in the clipboard in the expected format.
     */
    private fun checkClipboardForValidInput() {
        val clipboardManager =
            (activity?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        val clipboardString =
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        // if clipboard contains at least 1 emoji, then display paste emoji banner
        if (clipboardRegex.matches(clipboardString)) {
            val input = clipboardString.split("::")
            ui.publicKeyHexEditText.setText(input[0])
            ui.addressEditText.setText(input[1])
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        // validate public key
        val publicKeyHex = ui.publicKeyHexEditText.editableText.toString()
        if (!publicKeyRegex.matches(publicKeyHex)) {
            isValid = false
            ui.publicKeyHexEditText.background = drawable(base_node_config_edit_text_invalid_bg)
            ui.invalidPublicKeyHexTextView.visible()
        } else {
            ui.publicKeyHexEditText.background = drawable(base_node_config_edit_text_bg)
            ui.invalidPublicKeyHexTextView.invisible()
        }
        // validate address
        val address = ui.addressEditText.editableText.toString()
        if (!addressRegex.matches(address)) {
            isValid = false
            ui.addressEditText.background = drawable(base_node_config_edit_text_invalid_bg)
            ui.invalidAddressTextView.visible()
        } else {
            ui.addressEditText.background = drawable(base_node_config_edit_text_bg)
            ui.invalidAddressTextView.invisible()
        }
        return isValid
    }

    private fun saveButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        // validate
        if (!validate()) {
            return
        }
        val publicKeyHex = ui.publicKeyHexEditText.editableText.toString()
        val address = ui.addressEditText.editableText.toString()
        ui.saveButton.invisible()
        ui.progressBar.visible()
        AsyncTask.execute {
            addBaseNodePeer(publicKeyHex, address)
        }
    }

    private fun addBaseNodePeer(publicKeyHex: String, address: String) {
        val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
        val success = FFIWallet.instance!!.addBaseNodePeer(baseNodeKeyFFI, address)
        baseNodeKeyFFI.destroy()
        ui.rootView.post {
            if (success) {
                addBaseNodePeerSuccessful(publicKeyHex, address)
                // show toast
            } else {
                addBaseNodePeerFailed()
            }
        }
    }

    private fun addBaseNodePeerSuccessful(publicKeyHex: String, address: String) {
        val mActivity = activity ?: return
        // clear input
        ui.publicKeyHexEditText.setText("")
        ui.addressEditText.setText("")
        // update UI
        ui.publicKeyHexTextView.text = publicKeyHex
        ui.addressTextView.text = address
        // update app-wide variables
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        // UI
        ui.saveButton.visible()
        ui.progressBar.gone()
        // show toast
        Toast.makeText(
            mActivity,
            R.string.debug_edit_base_node_successful,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun addBaseNodePeerFailed() {
        val mActivity = activity ?: return
        // UI update
        ui.saveButton.visible()
        ui.progressBar.gone()
        // show toast
        Toast.makeText(
            mActivity,
            R.string.debug_edit_base_node_failed,
            Toast.LENGTH_LONG
        ).show()
    }

}
