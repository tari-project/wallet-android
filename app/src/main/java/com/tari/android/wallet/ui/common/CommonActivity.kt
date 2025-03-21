package com.tari.android.wallet.ui.common

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.screen.debug.DebugNavigation
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity
import com.tari.android.wallet.ui.screen.settings.allSettings.TariVersionModel
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.addEnterLeftAnimation
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.safeCastTo
import com.tari.android.wallet.util.extension.string

abstract class CommonActivity<VM : CommonViewModel> : AppCompatActivity(), ShakeDetector.Listener, FragmentPoppedListener {

    private var containerId: Int? = null

    lateinit var viewModel: VM

    private val dialogHandler: DialogHandler
        get() = viewModel

    private val shakeDetector by lazy { ShakeDetector(this) }

    val logger: Printer
        get() = Logger.t(this::class.simpleName)

    private val screenCaptureCallback: ScreenCaptureCallback? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            ScreenCaptureCallback { viewModel.onScreenCaptured() }
        } else null

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
    }

    fun subscribeToCommon(commonViewModel: CommonViewModel) = with(commonViewModel) {
        observe(backPressed) { onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(this@CommonActivity, it)) }

        observe(showToast) { TariToast(this@CommonActivity, it) }

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
        }
        setTheme(themeStyle)
    }

    override fun onStart() {
        (getSystemService(SENSOR_SERVICE) as? SensorManager)?.let(shakeDetector::start)
        super.onStart()
        screenCaptureCallback?.let { registerScreenCaptureCallback(mainExecutor, it) }
    }

    override fun onResume() {
        super.onResume()

        viewModel.tariNavigator.currentActivity = this@CommonActivity

        if (viewModel.tariSettingsSharedRepository.currentTheme != viewModel.currentTheme) {
            recreate()
        }
    }

    override fun onStop() {
        shakeDetector.stop()
        super.onStop()
        screenCaptureCallback?.let { unregisterScreenCaptureCallback(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        viewModel.processIntentDeepLink(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            logger.t(LoggerTags.Navigation.name).i(this::class.simpleName + " has been started")
        }
    }

    protected fun setContainerId(id: Int) {
        containerId = id
    }

    fun addFragment(fragment: CommonFragment<*>, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) {
        bundle?.let { fragment.arguments = it }
        if (supportFragmentManager.isDestroyed) return
        if (containerId == null) error("Container id is not set while adding fragment ${fragment::class.java.simpleName}")
        val transaction = supportFragmentManager.beginTransaction()
        if (withAnimation) {
            transaction.addEnterLeftAnimation()
        }
        transaction.add(containerId!!, fragment, fragment::class.java.simpleName)
        if (!isRoot) {
            transaction.addToBackStack(null)
        }
        fragment.setFragmentPoppedListener(this)
        transaction.commitAllowingStateLoss()
    }

    fun popUpTo(tag: String) {
        viewModel.logger.i(
            "popUpTo $tag\n" +
                    "popUpTo:last ${supportFragmentManager.fragments.last().tag}\n" +
                    "popUpTo:all ${supportFragmentManager.fragments.map { it.tag }.joinToString(", ")}\n" +
                    "popUpTo:all ${supportFragmentManager.fragments.map { it::class.java }.joinToString(", ")}"
        )
        while (supportFragmentManager.fragments.last()::class.java.simpleName != tag && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onFragmentPopped(fragmentClass: Class<out Fragment>) {
        supportFragmentManager.fragments
            .mapNotNull { it.safeCastTo<CommonFragment<*>>() }
            .forEach { fragment -> fragment.onFragmentPopped(fragmentClass) }
    }

    override fun hearShake() = showDebugDialog()

    private fun showDebugDialog() {
        val versionInfo = TariVersionModel(viewModel.networkRepository).versionInfo

        dialogHandler.showModularDialog(
            ModularDialogArgs(
                dialogId = ModularDialogArgs.DialogId.DEBUG_MENU,
                modules = listOfNotNull(
                    HeadModule(getString(R.string.debug_dialog_title)),
                    SpaceModule(8),
                    OptionModule(getString(R.string.debug_dialog_logs)) { openActivity(DebugNavigation.Logs) },
                    OptionModule(getString(R.string.debug_dialog_report)) { openActivity(DebugNavigation.BugReport) },
                    OptionModule(getString(R.string.debug_dialog_connection_status)) {
                        dialogHandler.hideDialog(ModularDialogArgs.DialogId.DEBUG_MENU)
                        dialogHandler.showConnectionStatusDialog()
                    },
                    OptionModule(getString(R.string.debug_dialog_sample_design_system)) { openActivity(DebugNavigation.SampleDesignSystem) }
                        .takeIf { DebugConfig.isDebug() },
                    BodyModule(versionInfo),
                    ButtonModule(getString(R.string.common_close), ButtonStyle.Close),
                ),
            )
        )
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
        dialogHandler.hideDialog()
        DebugActivity.launch(this, navigation)
    }
}
