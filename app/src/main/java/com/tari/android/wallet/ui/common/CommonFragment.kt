package com.tari.android.wallet.ui.common

import android.animation.Animator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import com.tari.android.wallet.ui.dialog.modular.InputModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.extension.dataIfOk
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.parcelable
import com.tari.android.wallet.util.extension.removeListenersAndCancel

abstract class CommonFragment<VM : CommonViewModel> : Fragment(), FragmentPoppedListener {

    private lateinit var clipboardManager: ClipboardManager

    lateinit var viewModel: VM

    protected val dialogHandler: DialogHandler
        get() = viewModel

    protected val animations = mutableListOf<Animator>()

    val logger: Printer
        get() = Logger.t(this::class.simpleName)

    //TODO make viewModel not lateinit. Sometimes it's not initialized in time and causes crashes, so we need to check if it's initialized
    private val blockScreenRecording
        get() = !BuildConfig.DEBUG &&
                (screenRecordingAlwaysDisable() || !(this::viewModel.isInitialized) || !viewModel.tariSettingsSharedRepository.screenRecordingTurnedOn)

    private var fragmentPoppedListener: FragmentPoppedListener? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.all { it.value }) {
            viewModel.permissionManager.grantedAction()
        } else {
            viewModel.permissionManager.showPermissionRequiredDialog(results.filter { !it.value }.map { it.key })
        }
    }

    private val scanQrCode = registerForActivityResult(StartActivityForResult()) { result ->
        val qrDeepLink = result.dataIfOk()?.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return@registerForActivityResult
        viewModel.handleDeeplink(qrDeepLink)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            logger.t(LoggerTags.Navigation.name).i(this::class.simpleName + " has been started")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //to eliminate click through fragments
        view.isClickable = true
        view.isFocusable = true

        clipboardManager = DiContainer.appComponent.getClipboardManager()
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded) return

        if (blockScreenRecording) {
            requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        animations.forEach { it.removeListenersAndCancel() }
        animations.clear()
    }

    override fun onDetach() {
        super.onDetach()

        fragmentPoppedListener?.onFragmentPopped(javaClass)
    }

    override fun onFragmentPopped(fragmentClass: Class<out Fragment>) {
        // implement in subclass
    }

    protected open fun screenRecordingAlwaysDisable() = false

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonFragment.viewModel = this

        subscribeVM(viewModel)

        dialogManager = viewModel.dialogManager
    }


    fun startQrScanner(source: QrScannerSource) {
        scanQrCode.launch(QrScannerActivity.newIntent(requireContext(), source))
    }

    fun setFragmentPoppedListener(listener: FragmentPoppedListener) {
        fragmentPoppedListener = listener
    }

    fun ensureIsAdded(action: () -> Unit) {
        if (isAdded && context != null) {
            action()
        }
    }

    fun <VM : CommonViewModel> subscribeVM(viewModel: VM) = with(viewModel) {
        observe(backPressed) { requireActivity().onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) }

        observe(copyToClipboard) { copy(it) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(requireActivity(), it)) }

        observe(inputDialog) { dialogManager.replace(InputModularDialog(requireActivity(), it)) }

        observe(permissionManager.checkForPermission) {
            launcher.launch(it.toTypedArray())
        }

        observe(permissionManager.openSettings) { openSettings() }

        observe(permissionManager.dialog) { dialogManager.replace(ModularDialog(requireActivity(), it)) }
    }

    private fun openSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val packageName = requireContext().packageName
            data = Uri.fromParts("package", packageName, null)
            ContextCompat.startActivity(requireActivity(), this, Bundle())
        }
    }

    protected fun doOnBackPressed(onBackPressedAction: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedAction()
            }
        })
    }

    private fun copy(clipboardArgs: ClipboardArgs) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(clipboardArgs.clipLabel, clipboardArgs.clipText))
        Toast.makeText(requireContext(), clipboardArgs.toastMessage, Toast.LENGTH_LONG).show()
    }
}

/**
 * Interface for listening to fragment pop events and performing actions once a fragment is popped.
 */
interface FragmentPoppedListener {
    fun onFragmentPopped(fragmentClass: Class<out Fragment>)
}
