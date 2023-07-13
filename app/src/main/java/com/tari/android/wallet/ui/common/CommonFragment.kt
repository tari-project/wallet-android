package com.tari.android.wallet.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.R
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.permission.PermissionManagerUI
import com.tari.android.wallet.ui.component.mainList.MutedBackPressedCallback
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.modular.InputModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.extension.string

abstract class CommonFragment<Binding : ViewBinding, VM : CommonViewModel> : Fragment() {

    lateinit var clipboardManager: ClipboardManager

    private val dialogManager = DialogManager()

    val permissionManagerUI = PermissionManagerUI(this)

    protected var blockingBackPressDispatcher = MutedBackPressedCallback(false)

    protected lateinit var ui: Binding

    lateinit var viewModel: VM

    val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.all { it.value }) {
            permissionManagerUI.grantedAction()
        } else {
            permissionManagerUI.notGrantedAction(it.filter { !it.value }.map { it.key })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //to eliminate click throuhg fragments
        view.isClickable = true
        view.isFocusable = true

        clipboardManager = DiContainer.appComponent.getClipboardManager()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dialogManager.context = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, blockingBackPressDispatcher)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonFragment.viewModel = this

        subscribeVM(viewModel)
    }

    fun <VM : CommonViewModel> subscribeVM(viewModel: VM) = with(viewModel) {
        observe(backPressed) { requireActivity().onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(copyToClipboard) { copy(it) }

        observe(modularDialog) { dialogManager.replace(ModularDialog(requireContext(), it)) }

        observe(inputDialog) { dialogManager.replace(InputModularDialog(requireContext(), it)) }

        observe(loadingDialog) { dialogManager.handleProgress(it) }

        observe(dismissDialog) { dialogManager.dismiss() }

        observe(showToast) { TariToast(requireContext(), it) }

        observe(blockedBackPressed) {
            blockingBackPressDispatcher.isEnabled = it
        }

        observe(navigation) { viewModel.tariNavigator.navigate(it) }

        observe(permissionManager.checkForPermission) {
            permissionManagerUI.grantedAction = { viewModel.permissionManager.permissionAction?.invoke() }
            permissionManagerUI.notGrantedAction = { viewModel.permissionManager.showPermissionRequiredDialog(it) }
            launcher.launch(it.toTypedArray())
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
        dialogManager.dismiss()
        super.onDestroy()
    }
}

