package com.tari.android.wallet.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.mainList.MutedBackPressedCallback
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.InputModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.extension.string

abstract class CommonFragment<Binding : ViewBinding, VM : CommonViewModel> : Fragment(), FragmentPoppedListener {

    private lateinit var clipboardManager: ClipboardManager

    protected var blockingBackPressDispatcher = MutedBackPressedCallback(false)

    protected lateinit var ui: Binding

    lateinit var viewModel: VM

    private var dialogManager: DialogManager? = null

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

    protected open fun screenRecordingAlwaysDisable() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //to eliminate click through fragments
        view.isClickable = true
        view.isFocusable = true

        clipboardManager = DiContainer.appComponent.getClipboardManager()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, blockingBackPressDispatcher)

        return super.onCreateView(inflater, container, savedInstanceState)
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

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonFragment.viewModel = this

        subscribeVM(viewModel)

        dialogManager = viewModel.dialogManager
    }

    override fun onDetach() {
        super.onDetach()

        fragmentPoppedListener?.onFragmentPopped(javaClass)
    }

    override fun onFragmentPopped(fragmentClass: Class<out Fragment>) {
        // implement in subclass
    }

    fun setFragmentPoppedListener(listener: FragmentPoppedListener) {
        fragmentPoppedListener = listener
    }


    fun <VM : CommonViewModel> subscribeVM(viewModel: VM) = with(viewModel) {
        observe(backPressed) { requireActivity().onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(copyToClipboard) { copy(it) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(requireContext(), it)) }

        observe(inputDialog) { dialogManager.replace(InputModularDialog(requireContext(), it)) }

        observe(loadingDialog) { progressDialogArgs ->
            if (progressDialogArgs.isShow) {
                dialogManager.replace(TariProgressDialog(requireContext(), progressDialogArgs))
            } else {
                dialogManager.dismiss()
            }
        }

        observe(showToast) { TariToast(requireContext(), it) }

        observe(blockedBackPressed) {
            blockingBackPressDispatcher.isEnabled = it
        }

        observe(navigation) { viewModel.tariNavigator.navigate(it) }

        observe(permissionManager.checkForPermission) {
            launcher.launch(it.toTypedArray())
        }

        observe(permissionManager.openSettings) { openSettings() }

        observe(permissionManager.dialog) { dialogManager.replace(ModularDialog(requireContext(), it)) }
    }

    private fun openSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val packageName = requireContext().packageName
            data = Uri.fromParts("package", packageName, null)
            ContextCompat.startActivity(requireActivity(), this, Bundle())
        }
    }

    protected fun changeOnBackPressed(isBlocked: Boolean) {
        blockingBackPressDispatcher.isEnabled = false
        blockingBackPressDispatcher = MutedBackPressedCallback(isBlocked)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
    }

    private fun copy(clipboardArgs: ClipboardArgs) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(clipboardArgs.clipLabel, clipboardArgs.clipText))
        TariToast(requireContext(), TariToastArgs(clipboardArgs.toastMessage, Toast.LENGTH_LONG))
    }

    protected fun shareViaText(text: String) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        shareIntent.type = "text/plain"
        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            TariToast(requireContext(), TariToastArgs(string(R.string.store_no_application_to_open_the_link_error), Toast.LENGTH_LONG))
        }
    }

    fun ensureIsAdded(action: () -> Unit) {
        if (isAdded && context != null) {
            action()
        }
    }

    override fun onDestroy() {
        dialogManager?.dismiss()
        super.onDestroy()
    }
}

/**
 * Interface for listening to fragment pop events and performing actions once a fragment is popped.
 */
interface FragmentPoppedListener {
    fun onFragmentPopped(fragmentClass: Class<out Fragment>)
}
