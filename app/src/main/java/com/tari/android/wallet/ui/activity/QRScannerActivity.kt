package com.tari.android.wallet.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.OnClick
import com.budiyev.android.codescanner.*
import com.google.zxing.BarcodeFormat
import com.tari.android.wallet.R

private const val REQUEST_CAMERA_PERMISSION = 102
const val EXTRA_QR_DATA = "extra_qr_text"

/**
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
super.onActivityResult(requestCode, resultCode, data)

if (requestCode == REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
val text = data.getStringExtra(EXTRA_QR_DATA)
Toast.makeText(this, "Scan result: $text", Toast.LENGTH_LONG).show()
}
}
 */

class QRScannerActivity : BaseActivity() {

    @BindView(R.id.scanner_view)
    lateinit var scannerView: CodeScannerView

    private lateinit var codeScanner: CodeScanner

    override val contentViewId = R.layout.activity_qr_scanner

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startScanning()
        }
    }

    @OnClick(R.id.qr_close)
    fun onCloseQrScannerClick() {
        finish()
        overridePendingTransition(0, R.anim.slide_down)
    }

    private fun startScanning() {
        codeScanner = CodeScanner(this, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {
            val intent = Intent()
            intent.putExtra(EXTRA_QR_DATA, it.text)
            setResult(Activity.RESULT_OK, intent)
            finish()
            overridePendingTransition(0, R.anim.slide_down)
        }

        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(
                    this, R.string.failed_init_camera_message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    R.string.camera_permission_denied_message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }

}