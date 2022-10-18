package com.tari.android.wallet.ui.common

import android.content.Intent
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.fragment.debug.activity.DebugActivity
import yat.android.lib.YatIntegration

abstract class CommonActivity<Binding : ViewBinding, VM : CommonViewModel> : AppCompatActivity(), ShakeDetector.Listener {

    private var currentDialog: TariDialog? = null

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

    private val shakeDetector by lazy { ShakeDetector(this) }

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonActivity.viewModel = viewModel

        observe(backPressed) { onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(modularDialog) { replaceDialog(ModularDialog(this@CommonActivity, it)) }

        observe(dismissDialog) { currentDialog?.dismiss() }

        observe(loadingDialog) { if (it.isShow) replaceDialog(TariProgressDialog(this@CommonActivity, it)) else currentDialog?.dismiss() }
    }

    override fun onStart() {
        (getSystemService(SENSOR_SERVICE) as? SensorManager)?.let(shakeDetector::start)
        super.onStart()
    }

    override fun onStop() {
        shakeDetector.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { deepLink -> YatIntegration.processDeepLink(this, deepLink) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun replaceDialog(dialog: TariDialog) {
        val currentLoadingDialog = currentDialog as? TariProgressDialog
        if (currentLoadingDialog != null && currentLoadingDialog.isShowing() && dialog is TariProgressDialog) {
            (currentDialog as TariProgressDialog).applyArgs(dialog.progressDialogArgs)
            return
        }
        val currentModularDialog = currentDialog as? ModularDialog
        val newModularDialog = dialog as? ModularDialog
        if (currentModularDialog != null && newModularDialog != null && currentModularDialog.args::class.java == newModularDialog.args::class.java
            && currentModularDialog.isShowing()
        ) {
            currentModularDialog.applyArgs(newModularDialog.args)
            return
        }

        if (newModularDialog?.args?.dialogArgs?.isRefreshing == true) {
            return
        }

        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }

    override fun hearShake() = openDebugActivity()

    fun openDebugActivity() {
        val intent = Intent(this, DebugActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}


