package com.tari.android.wallet.ui.screen.settings.torBridges.customBridges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentCustomTorBridgesBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.setOnThrottledClickListener

class CustomTorBridgesFragment : CommonXmlFragment<FragmentCustomTorBridgesBinding, CustomTorBridgesViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentCustomTorBridgesBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: CustomTorBridgesViewModel by viewModels()
        bindViewModel(viewModel)

        setupViews()
    }

    private fun setupViews() = with(ui) {
        requestBridgesCta.setOnThrottledClickListener { viewModel.openRequestPage() }
        scanQrCta.setOnThrottledClickListener {
            startQrScanner(QrScannerSource.TorBridges)
        }
        uploadQrCta.setOnThrottledClickListener { viewModel.navigateToUploadQr() }
        val actionArgs = TariToolbarActionArg(title = requireContext().getString(R.string.tor_bridges_connect)) {
            viewModel.connect(torBridgeConfiguration.ui.editText.text.toString())
        }
        toolbar.setRightArgs(actionArgs)

        observe(viewModel.text) { torBridgeConfiguration.ui.editText.setText(it) }
    }
}