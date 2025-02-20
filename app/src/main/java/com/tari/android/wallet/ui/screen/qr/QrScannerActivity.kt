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
package com.tari.android.wallet.ui.screen.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.BarcodeFormat
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.databinding.ActivityQrScannerBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.ui.common.CommonXmlActivity
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.setVisible

/**
 * QR code scanner activity - used to add a recipient by QR code.
 *
 * @author The Tari Development Team
 */
class QrScannerActivity : CommonXmlActivity<ActivityQrScannerBinding, QrScannerViewModel>() {

    companion object {
        /**
         * Activity result code.
         */
        const val REQUEST_QR_SCANNER = 101

        fun startScanner(fragment: Fragment, source: QrScannerSource) {
            val intent = Intent(fragment.requireActivity(), QrScannerActivity::class.java)
            intent.putExtra(EXTRA_QR_DATA_SOURCE, source)
            fragment.startActivityForResult(intent, REQUEST_QR_SCANNER)
        }

        private const val REQUEST_CAMERA_PERMISSION = 102
        const val EXTRA_DEEPLINK = "EXTRA_DEEPLINK"
        const val EXTRA_QR_DATA_SOURCE = "EXTRA_QR_DATA_SOURCE"
    }

    private var codeScanner: CodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityQrScannerBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: QrScannerViewModel by viewModels()
        bindViewModel(viewModel)

        subscribeUI()
        setupUi()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            startScanning()
        }
    }

    private fun subscribeUI() {
        collectFlow(viewModel.uiState) { uiState ->
            ui.errorContainer.setVisible(uiState.scanError)
            ui.alternativeText.text = uiState.alternativeText
            ui.alternativeContainer.setVisible(uiState.alternativeText.isNotEmpty())
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is QrScannerModel.Effect.FinishWithResult -> finishWithResult(effect.deepLink)
                is QrScannerModel.Effect.ProceedScan -> codeScanner?.startPreview()
            }
        }
    }

    private fun setupUi() = with(ui) {
        qrCloseView.setOnClickListener {
            finish()
        }

        alternativeApply.setOnClickListener { viewModel.onAlternativeApply(this@QrScannerActivity) }
        alternativeDeny.setOnClickListener { viewModel.onAlternativeDeny() }
        retryButton.setOnClickListener { viewModel.onRetry() }
    }

    private fun startScanning() {
        codeScanner = CodeScanner(this, ui.scannerView).apply {
            camera = CodeScanner.CAMERA_BACK
            formats = listOf(BarcodeFormat.QR_CODE)
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
        }

        codeScanner?.decodeCallback = DecodeCallback { viewModel.onScanResult(it.text) }

        codeScanner?.errorCallback = ErrorCallback {
            runOnUiThread { TariToast(this, TariToastArgs(getString(R.string.add_recipient_failed_init_camera_message), Toast.LENGTH_LONG)) }
        }

        ui.scannerView.setOnClickListener { codeScanner?.startPreview() }
    }

    private fun finishWithResult(deepLink: DeepLink) {
        val intent = Intent()
        intent.putExtra(EXTRA_DEEPLINK, deepLink)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                TariToast(this, TariToastArgs(getString(R.string.add_recipient_camera_permission_denied_message), Toast.LENGTH_LONG))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner?.startPreview()
    }

    override fun onPause() {
        codeScanner?.releaseResources()
        super.onPause()
    }
}