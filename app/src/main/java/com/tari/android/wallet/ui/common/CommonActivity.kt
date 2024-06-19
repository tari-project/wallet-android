package com.tari.android.wallet.ui.common

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
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

abstract class CommonActivity<Binding : ViewBinding, VM : CommonViewModel> : AppCompatActivity(), ShakeDetector.Listener, FragmentPoppedListener {

    private var containerId: Int? = null

    lateinit var ui: Binding

    lateinit var viewModel: VM

    private  var dialogManager : DialogManager? = null

    private val shakeDetector by lazy { ShakeDetector(this) }

    private val connectionStateViewModel: ConnectionIndicatorViewModel by viewModels()

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val (granted, nonGranted) = permissions.toList().partition { it.second }
        if (nonGranted.isEmpty()) {
            viewModel.permissionManager.grantedAction()
        } else {
            viewModel.permissionManager.showPermissionRequiredDialog(nonGranted.map { it.first }.toList())
        }
    }

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonActivity.viewModel = viewModel

        setTariTheme(viewModel.tariSettingsSharedRepository.currentTheme)

        subscribeToCommon(viewModel)

        dialogManager = viewModel.dialogManager
    }

    fun subscribeToCommon(commonViewModel: CommonViewModel) = with(commonViewModel) {
        observe(backPressed) { onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(this@CommonActivity, it)) }

        observe(loadingDialog) { progressDialogArgs ->
            if (progressDialogArgs.isShow) {
                dialogManager.replace(TariProgressDialog(this@CommonActivity, progressDialogArgs))
            } else {
                dialogManager.dismiss()
            }
        }

        observe(showToast) { TariToast(this@CommonActivity, it) }

        observe(navigation) { commonViewModel.tariNavigator.navigate(it) }

        observe(permissionManager.checkForPermission) {
            launcher.launch(it.toTypedArray())
        }

        observe(permissionManager.openSettings) { openSettings() }

        observe(permissionManager.dialog) { dialogManager.replace(ModularDialog(this@CommonActivity, it)) }
    }

    private fun openSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val packageName = this@CommonActivity.packageName
            data = Uri.fromParts("package", packageName, null)
            ContextCompat.startActivity(this@CommonActivity, this, Bundle())
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

    fun <T : Activity> launch(destination: Class<T>) {
        val intent = Intent(this, destination)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        this.intent.data?.let(intent::setData)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        viewModel.tariNavigator.activity = this@CommonActivity

        if (viewModel.tariSettingsSharedRepository.currentTheme != viewModel.currentTheme.value)
            recreate()
    }

    override fun onStop() {
        shakeDetector.stop()
        dialogManager?.dismiss()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { deepLink -> YatIntegration.processDeepLink(this, deepLink) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscribeToCommon(connectionStateViewModel)
    }

    protected fun setContainerId(id: Int) {
        containerId = id
    }

    fun addFragment(fragment: CommonFragment<*, *>, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) {
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
        fragment.setFragmentPoppedListener(this)
        transaction.commit()
    }

    fun popUpTo(tag: String) {
        viewModel.logger.i("popUpTo $tag")
        viewModel.logger.i("popUpTo:last ${supportFragmentManager.fragments.last().tag}")
        viewModel.logger.i("popUpTo:all ${supportFragmentManager.fragments.map { it.tag }.joinToString(", ")}")
        viewModel.logger.i("popUpTo:all ${supportFragmentManager.fragments.map { it::class.java }.joinToString(", ")}")
        while (supportFragmentManager.fragments.last()::class.java.simpleName != tag && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onFragmentPopped(fragmentClass: Class<out Fragment>) {
        supportFragmentManager.fragments
            .mapNotNull { it.safeCastTo<CommonFragment<*, *>>() }
            .forEach { fragment -> fragment.onFragmentPopped(fragmentClass) }
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
                    dialogManager?.dismiss()
                    connectionStateViewModel.showStatesDialog()
                },
                BodyModule(versionInfo),
                ButtonModule(getString(R.string.common_close), ButtonStyle.Close),
            )
        )
        dialogManager?.replace(ModularDialog(this, modularDialogArgs))
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
        dialogManager?.dismiss()
        DebugActivity.launch(this, navigation)
    }
}

