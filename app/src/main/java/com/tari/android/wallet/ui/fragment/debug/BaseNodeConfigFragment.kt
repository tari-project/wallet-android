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

import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_bg
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_invalid_bg
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.databinding.FragmentBaseNodeConfigBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.BaseNodeValidationResult
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Base node configuration debug fragment.
 * If you reach this fragment with valid base node data in the clipboard in format
 * PUBLIC_KEY_HEX::ADDRESS then the fragment will split the clipboard data and paste the values
 * to input fields.
 *
 * @author The Tari Development Team
 */
internal class BaseNodeConfigFragment : Fragment(), ServiceConnection {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var walletManager: WalletManager

    private val onion2ClipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion/[a-zA-Z2-7]{16}(:[0-9]+)?")
    private val onion3ClipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")
    private val publicKeyRegex = Regex("[a-zA-Z0-9]{64}")
    private val onion2AddressRegex = Regex("/onion/[a-zA-Z2-7]{16}(:[0-9]+)?")
    private val onion3AddressRegex = Regex("/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")

    private lateinit var ui: FragmentBaseNodeConfigBinding
    private lateinit var walletService: TariWalletService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentBaseNodeConfigBinding.inflate(inflater, container, false)
            .also { ui = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        bindToWalletService()
        setupUI()
        subscribeToEventBus()
    }

    override fun onStart() {
        super.onStart()
        checkClipboardForValidBaseNodeData()
    }

    override fun onDestroyView() {
        EventBus.unsubscribe(this)
        requireActivity().unbindService(this)
        super.onDestroyView()
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Wallet.BaseNodeSyncComplete>(this) {
            lifecycleScope.launch(Dispatchers.Main) {
                updateCurrentBaseNode()
            }
        }
        EventBus.subscribe<Event.Wallet.BaseNodeSyncStarted>(this) {
            lifecycleScope.launch(Dispatchers.Main) {
                updateCurrentBaseNode()
            }
        }
    }

    private fun bindToWalletService() {
        val bindIntent = Intent(requireActivity(), WalletService::class.java)
        requireActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("AddAmountFragment onServiceConnected")
        walletService = TariWalletService.Stub.asInterface(service)
        // Only binding UI if we have not passed `onDestroyView` line, which is a possibility
        setupUI()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("AddAmountFragment onServiceDisconnected")
        // No-op for now
    }

    private fun setupUI() {
        ui.progressBar.setColor(color(white))
        ui.apply {
            updateCurrentBaseNode()
            progressBar.gone()
            invalidPublicKeyHexTextView.invisible()
            invalidAddressTextView.invisible()
            resetButton.setOnClickListener { resetButtonClicked(it) }
            saveButton.setOnClickListener { saveButtonClicked(it) }
            publicKeyHexEditText.addTextChangedListener(
                onTextChanged = { _, _, _, _ -> onPublicKeyHexChanged() }
            )
            addressEditText.addTextChangedListener(
                onTextChanged = { _, _, _, _ -> onAddressChanged() }
            )
        }
    }

    private fun updateCurrentBaseNode() {
        val syncSuccessful = sharedPrefsWrapper.baseNodeLastSyncResult
        ui.syncStatusTextView.text = when (syncSuccessful) {
            null -> {
                string(R.string.debug_base_node_syncing)
            }
            BaseNodeValidationResult.SUCCESS -> {
                string(R.string.debug_base_node_sync_successful)
            }
            else -> {
                string(R.string.debug_base_node_sync_failed)
            }
        }
        if (sharedPrefsWrapper.baseNodeIsUserCustom) {
            ui.nameTextView.text = string(R.string.debug_base_node_custom)
            ui.resetButton.visible()
        } else {
            ui.nameTextView.text = sharedPrefsWrapper.baseNodeName
            ui.resetButton.gone()
        }
        ui.publicKeyHexTextView.text = sharedPrefsWrapper.baseNodePublicKeyHex
        ui.addressTextView.text = sharedPrefsWrapper.baseNodeAddress
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
    private fun checkClipboardForValidBaseNodeData() {
        val clipboardManager =
            (activity?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        val clipboardString =
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        // if clipboard contains at least 1 emoji, then display paste emoji banner
        if (onion3ClipboardRegex.matches(clipboardString)) {
            val input = clipboardString.split("::")
            ui.publicKeyHexEditText.setText(input[0])
            ui.addressEditText.setText(input[1])
        } else if (onion2ClipboardRegex.matches(clipboardString)) {
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
        if (!onion3AddressRegex.matches(address) && !onion2AddressRegex.matches(address)) {
            isValid = false
            ui.addressEditText.background = drawable(base_node_config_edit_text_invalid_bg)
            ui.invalidAddressTextView.visible()
        } else {
            ui.addressEditText.background = drawable(base_node_config_edit_text_bg)
            ui.invalidAddressTextView.invisible()
        }
        return isValid
    }

    private fun resetButtonClicked(view: View) {
        view.temporarilyDisableClick()
        sharedPrefsWrapper.baseNodeIsUserCustom = false
        sharedPrefsWrapper.baseNodeLastSyncResult = null
        lifecycleScope.launch(Dispatchers.IO) {
            walletManager.setNextBaseNode()
            walletService.startBaseNodeSync(WalletError())
            withContext(Dispatchers.Main) {
                updateCurrentBaseNode()
            }
        }
    }

    private fun saveButtonClicked(view: View) {
        view.temporarilyDisableClick()
        if (!validate()) return
        val publicKeyHex = ui.publicKeyHexEditText.editableText.toString()
        val address = ui.addressEditText.editableText.toString()
        sharedPrefsWrapper.baseNodeIsUserCustom = true
        sharedPrefsWrapper.baseNodeLastSyncResult = null
        sharedPrefsWrapper.baseNodeName = null
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        ui.saveButton.invisible()
        ui.progressBar.visible()
        lifecycleScope.launch(Dispatchers.IO) {
            addCustomBaseNodePeer(publicKeyHex, address)
        }
    }

    private suspend fun addCustomBaseNodePeer(publicKeyHex: String, address: String) {
        val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
        val success = try {
            val wallet = FFIWallet.instance!!
            wallet.addBaseNodePeer(baseNodeKeyFFI, address)
            true
        } catch (exception: Exception) {
            false
        }
        baseNodeKeyFFI.destroy()
        if (success) {
            withContext(Dispatchers.Main) {
                addBaseNodePeerSuccessful(publicKeyHex, address)
            }
        } else {
            walletManager.setNextBaseNode()
            walletService.startBaseNodeSync(WalletError())
            withContext(Dispatchers.Main) {
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
        updateCurrentBaseNode()
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
        updateCurrentBaseNode()
    }

}
