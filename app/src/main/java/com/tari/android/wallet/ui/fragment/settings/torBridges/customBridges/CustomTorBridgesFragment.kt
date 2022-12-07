package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentCustomTorBridgesBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity

class CustomTorBridgesFragment : CommonFragment<FragmentCustomTorBridgesBinding, CustomTorBridgesViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentCustomTorBridgesBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: CustomTorBridgesViewModel by viewModels()
        bindViewModel(viewModel)

        setupViews()

        observeUI()
    }

    private fun setupViews() = with(ui) {
        requestBridgesCta.setOnThrottledClickListener { viewModel.openRequestPage() }
        scanQrCta.setOnThrottledClickListener { viewModel.navigateToScanQr() }
        uploadQrCta.setOnThrottledClickListener { viewModel.navigateToUploadQr() }
        toolbar.rightAction = { viewModel.connect(torBridgeConfiguration.text.toString()) }
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }
    }

    private fun processNavigation(navigation: CustomBridgeNavigation) {
        when (navigation) {
            CustomBridgeNavigation.ScanQrCode -> {
                val intent = Intent(requireContext(), QRScannerActivity::class.java)
                startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
                requireActivity().overridePendingTransition(R.anim.slide_up, 0)
            }
            CustomBridgeNavigation.UploadQrCode -> Unit
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrData = data.getStringExtra(QRScannerActivity.EXTRA_QR_DATA) ?: return
            val bridgeList = Gson().fromJson(qrData, StringList::class.java)
            ui.torBridgeConfiguration.setText(bridgeList.joinToString("\n"))
        }
    }

    class StringList : ArrayList<String>()
}

