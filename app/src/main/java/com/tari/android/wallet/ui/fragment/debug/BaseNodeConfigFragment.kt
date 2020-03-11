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
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import butterknife.*
import com.tari.android.wallet.R
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFITestWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ui.fragment.BaseFragment
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
internal class BaseNodeConfigFragment : BaseFragment() {

    @BindView(R.id.base_node_config_vw_root)
    lateinit var rootView: View

    @BindView(R.id.base_node_config_txt_public_key_hex)
    lateinit var publicKeyHexTextView: TextView

    @BindView(R.id.base_node_config_txt_address)
    lateinit var addressTextView: TextView

    @BindView(R.id.base_node_config_edit_public_key)
    lateinit var publicKeyHexEditText: TextView

    @BindView(R.id.base_node_config_txt_invalid_public_key)
    lateinit var invalidPublicKeyHexTextView: TextView

    @BindView(R.id.base_node_config_edit_address)
    lateinit var addressEditText: TextView

    @BindView(R.id.base_node_config_txt_invalid_address)
    lateinit var invalidAddressTextView: TextView

    @BindView(R.id.base_node_config_btn_save)
    lateinit var saveButton: Button

    @BindView(R.id.base_node_config_progress_bar)
    lateinit var progressBar: ProgressBar

    @BindDrawable(R.drawable.base_node_config_edit_text_bg)
    lateinit var editTextBgDrawable: Drawable

    @BindDrawable(R.drawable.base_node_config_edit_text_invalid_bg)
    lateinit var editTextInvalidBgDrawable: Drawable

    @BindColor(R.color.white)
    @JvmField
    var whiteColor: Int = 0

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var wallet: FFITestWallet

    private val clipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")
    private val publicKeyRegex = Regex("[a-zA-Z0-9]{64}")
    private val addressRegex = Regex("/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")

    override val contentViewId = R.layout.fragment_base_node_config

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        publicKeyHexTextView.text = sharedPrefsWrapper.baseNodePublicKeyHex
        addressTextView.text = sharedPrefsWrapper.baseNodeAddress
        UiUtil.setProgressBarColor(progressBar, whiteColor)
        progressBar.visibility = View.GONE
        invalidPublicKeyHexTextView.visibility = View.INVISIBLE
        invalidAddressTextView.visibility = View.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        checkClipboardForValidInput()
    }

    @OnTextChanged(R.id.base_node_config_edit_public_key)
    fun onPublicKeyHexChanged() {
        publicKeyHexEditText.background = editTextBgDrawable
        invalidPublicKeyHexTextView.visibility = View.INVISIBLE
    }

    @OnTextChanged(R.id.base_node_config_edit_address)
    fun onAddressChanged() {
        addressEditText.background = editTextBgDrawable
        invalidAddressTextView.visibility = View.INVISIBLE
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
            publicKeyHexEditText.text = input[0]
            addressEditText.text = input[1]
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        // validate public key
        val publicKeyHex = publicKeyHexEditText.editableText.toString()
        if (!publicKeyRegex.matches(publicKeyHex)) {
            isValid = false
            publicKeyHexEditText.background = editTextInvalidBgDrawable
            invalidPublicKeyHexTextView.visibility = View.VISIBLE
        } else {
            publicKeyHexEditText.background = editTextBgDrawable
            invalidPublicKeyHexTextView.visibility = View.INVISIBLE
        }
        // validate address
        val address = addressEditText.editableText.toString()
        if (!addressRegex.matches(address)) {
            isValid = false
            addressEditText.background = editTextInvalidBgDrawable
            invalidAddressTextView.visibility = View.VISIBLE
        } else {
            addressEditText.background = editTextBgDrawable
            invalidAddressTextView.visibility = View.INVISIBLE
        }
        return isValid
    }

    @OnClick(R.id.base_node_config_btn_save)
    fun saveButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        // validate
        if (!validate()) {
            return
        }
        val publicKeyHex = publicKeyHexEditText.editableText.toString()
        val address = addressEditText.editableText.toString()
        saveButton.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        AsyncTask.execute {
            addBaseNodePeer(publicKeyHex, address)
        }
    }

    private fun addBaseNodePeer(publicKeyHex: String, address: String) {
        val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
        val success = wallet.addBaseNodePeer(baseNodeKeyFFI, address)
        baseNodeKeyFFI.destroy()
        rootView.post {
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
        publicKeyHexEditText.text = ""
        addressEditText.text = ""
        // update UI
        publicKeyHexTextView.text = publicKeyHex
        addressTextView.text = address
        // update app-wide variables
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        // UI
        saveButton.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
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
        saveButton.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        // show toast
        Toast.makeText(
            mActivity,
            R.string.debug_edit_base_node_failed,
            Toast.LENGTH_LONG
        ).show()
    }

}
