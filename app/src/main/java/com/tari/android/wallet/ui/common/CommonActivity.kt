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
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModule
import com.tari.android.wallet.ui.fragment.settings.allSettings.TariVersionModel
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugNavigation
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

    override fun hearShake() = showDebugDialog()

    fun showDebugDialog() {
        val versionInfo = TariVersionModel(viewModel.networkRepository).versionInfo

        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(getString(R.string.debug_dialog_title)),
                OptionModule(getString(R.string.debug_dialog_logs)) { openActivity(DebugNavigation.Logs) },
                OptionModule(getString(R.string.debug_dialog_report)) { openActivity(DebugNavigation.BugReport) },
                OptionModule(getString(R.string.debug_dialog_connection_status)) { openActivity(DebugNavigation.ConnectionStatus) },
                BodyModule(versionInfo),
                ButtonModule(getString(R.string.common_close), ButtonStyle.Close),
            )
        )
        replaceDialog(ModularDialog(this, modularDialogArgs))
    }

    private fun openActivity(navigation: DebugNavigation) {
        currentDialog?.dismiss()
        DebugActivity.launch(this, navigation)
    }
}


