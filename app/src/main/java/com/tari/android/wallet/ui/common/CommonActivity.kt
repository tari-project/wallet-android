package com.tari.android.wallet.ui.common

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.permission.PermissionManagerActivityUI
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.extension.addEnterLeftAnimation
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.settings.allSettings.TariVersionModel
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugNavigation
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme
import yat.android.lib.YatIntegration

abstract class CommonActivity<Binding : ViewBinding, VM : CommonViewModel> : AppCompatActivity(), ShakeDetector.Listener {

    private val dialogManager = DialogManager()
    private var containerId: Int? = null

    lateinit var ui: Binding

    lateinit var viewModel: VM

    private val shakeDetector by lazy { ShakeDetector(this) }

    private val connectionStateViewModel: ConnectionIndicatorViewModel by viewModels()

    val permissionManagerUI = PermissionManagerActivityUI(this)

    val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.all { it.value }) {
            permissionManagerUI.grantedAction()
        } else {
            permissionManagerUI.notGrantedAction(it.filter { !it.value }.map { it.key })
        }
    }

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonActivity.viewModel = viewModel

        viewModel.tariNavigator.activity = this@CommonActivity

        setTariTheme(viewModel.tariSettingsSharedRepository.currentTheme!!)

        subscribeToCommon(viewModel)
    }

    fun subscribeToCommon(commonViewModel: CommonViewModel) = with(commonViewModel) {
        observe(backPressed) { onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(this@CommonActivity, it)) }

        observe(dismissDialog) { dialogManager.dismiss() }

        observe(loadingDialog) { dialogManager.handleProgress(it) }

        observe(showToast) { TariToast(this@CommonActivity, it) }

        observe(navigation) { commonViewModel.tariNavigator.navigate(it) }

        observe(permissionManager.checkForPermission) {
            permissionManagerUI.grantedAction = { commonViewModel.permissionManager.permissionAction?.invoke() }
            permissionManagerUI.notGrantedAction = { commonViewModel.permissionManager.showPermissionRequiredDialog(it) }
            launcher.launch(it.toTypedArray())
        }
    }

    private fun setTariTheme(theme: TariTheme) {
        val themeStyle = when (theme) {
            TariTheme.AppBased -> {
                when (resources.configuration.uiMode and UI_MODE_NIGHT_MASK) {
                    UI_MODE_NIGHT_YES -> R.style.AppTheme_Dark
                    else -> R.style.AppTheme_Light
                }
            }

            TariTheme.Light -> R.style.AppTheme_Light
            TariTheme.Dark -> R.style.AppTheme_Dark
            TariTheme.Purple -> R.style.AppTheme_Purple
        }
        setTheme(themeStyle)
    }

    override fun onStart() {
        (getSystemService(SENSOR_SERVICE) as? SensorManager)?.let(shakeDetector::start)
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.tariSettingsSharedRepository.currentTheme != viewModel.currentTheme.value)
            recreate()
    }

    override fun onStop() {
        shakeDetector.stop()
        dialogManager.dismiss()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { deepLink -> YatIntegration.processDeepLink(this, deepLink) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogManager.context = this
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        subscribeToCommon(connectionStateViewModel)
    }

    protected fun setContainerId(id: Int) {
        containerId = id
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    fun addFragment(fragment: Fragment, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) {
        bundle?.let { fragment.arguments = it }
        if (supportFragmentManager.isDestroyed) return
        val transaction = supportFragmentManager.beginTransaction()
        if (withAnimation) {
            transaction.addEnterLeftAnimation()
        }
        transaction.add(containerId!!, fragment, fragment::class.java.simpleName)
        if (!isRoot) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    fun popUpTo(tag: String) {
        while (supportFragmentManager.fragments.last().tag != tag && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun hearShake() = showDebugDialog()

    fun showDebugDialog() {
        val versionInfo = TariVersionModel(viewModel.networkRepository).versionInfo

        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(getString(R.string.debug_dialog_title)),
                SpaceModule(8),
                OptionModule(getString(R.string.debug_dialog_logs)) { openActivity(DebugNavigation.Logs) },
                OptionModule(getString(R.string.debug_dialog_report)) { openActivity(DebugNavigation.BugReport) },
                OptionModule(getString(R.string.debug_dialog_connection_status)) {
                    dialogManager.dismiss()
                    connectionStateViewModel.showStatesDialog()
                },
                BodyModule(versionInfo),
                ButtonModule(getString(R.string.common_close), ButtonStyle.Close),
            )
        )
        dialogManager.replace(ModularDialog(this, modularDialogArgs))
    }

    protected fun shareViaText(text: String) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        shareIntent.type = "text/plain"
        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            TariToast(this, TariToastArgs(string(R.string.store_no_application_to_open_the_link_error), Toast.LENGTH_LONG))
        }
    }

    private fun openActivity(navigation: DebugNavigation) {
        dialogManager.dismiss()
        DebugActivity.launch(this, navigation)
    }
}

