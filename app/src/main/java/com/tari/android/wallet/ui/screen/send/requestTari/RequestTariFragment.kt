package com.tari.android.wallet.ui.screen.send.requestTari

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentRequestTariBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.screen.send.addAmount.keyboard.KeyboardController
import com.tari.android.wallet.ui.screen.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.util.extension.hideKeyboard
import com.tari.android.wallet.util.extension.setOnThrottledClickListener
import com.tari.android.wallet.util.extension.setVisible


class RequestTariFragment : CommonXmlFragment<FragmentRequestTariBinding, RequestTariViewModel>() {

    companion object {
        fun newInstance(withToolbar: Boolean = true) = RequestTariFragment().apply {
            arguments = Bundle().apply { putBoolean("withToolbar", withToolbar) }
        }
    }

    private val keyboardController: KeyboardController = KeyboardController()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentRequestTariBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: RequestTariViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        subscribeUI()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().hideKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardController.onDestroy()
    }

    private fun subscribeUI() = Unit

    private fun setupUI() {

        val withToolbar = arguments?.getBoolean("withToolbar") != false
        ui.toolbar.setVisible(withToolbar)

        keyboardController.setup(requireContext(), AmountCheckRunnable(), ui.numpad, ui.amount)
        ui.shareButton.setOnThrottledClickListener { shareDeeplink(viewModel.getDeepLink(keyboardController.currentAmount)) }
        ui.generateButton.setOnThrottledClickListener { showQRCodeDialog(viewModel.getDeepLink(keyboardController.currentAmount)) }
    }

    private fun shareDeeplink(deeplink: String) {
        val subject = requireContext().getString(R.string.request_tari_share_text, keyboardController.currentAmount.formattedTariValue, deeplink)

        ShareCompat.IntentBuilder(requireContext())
            .setType("text/x-uri")
            .setText(deeplink)
            .setChooserTitle(subject)
            .startChooser()
    }

    private fun showQRCodeDialog(deeplink: String) {
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = true), listOf(
                ShareQrCodeModule(deeplink),
                ButtonModule(viewModel.resourceManager.getString(R.string.common_share), ButtonStyle.Normal) { shareDeeplink(deeplink) },
                ButtonModule(viewModel.resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )

        ModularDialog(requireActivity(), args).show()
    }

    private inner class AmountCheckRunnable : Runnable {
        override fun run() {
            val amount = keyboardController.currentAmount
            val isNotEmpty = amount.tariValue.toDouble() != 0.0

            ui.shareButton.isEnabled = isNotEmpty
            ui.generateButton.isEnabled = isNotEmpty
        }
    }
}