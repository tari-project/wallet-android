package com.tari.android.wallet.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.MutedBackPressedCallback
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog

abstract class CommonFragment<Binding : ViewBinding, VM : CommonViewModel> : Fragment() {

    lateinit var clipboardManager: ClipboardManager

    private var currentDialog: TariDialog? = null

    protected var blockingBackPressDispatcher = MutedBackPressedCallback(false)

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clipboardManager = DiContainer.appComponent.getClipboardManager()
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

        observe(modularDialog) { replaceDialog(ModularDialog(requireContext(), it)) }

        observe(loadingDialog) { if (it.isShow) replaceDialog(TariProgressDialog(requireContext(), it)) else currentDialog?.dismiss() }

        observe(dismissDialog) { currentDialog?.dismiss() }

        observe(blockedBackPressed) {
            blockingBackPressDispatcher.isEnabled = it
        }
    }

    protected fun changeOnBackPressed(isBlocked: Boolean) {
        blockingBackPressDispatcher.isEnabled = false
        blockingBackPressDispatcher = MutedBackPressedCallback(isBlocked)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
    }

    protected fun replaceDialog(dialog: TariDialog) {
        val currentLoadingDialog = currentDialog as? TariProgressDialog
        if (currentLoadingDialog != null && currentLoadingDialog.isShowing() && dialog is TariProgressDialog) {
            (currentDialog as TariProgressDialog).applyArgs(dialog.progressDialogArgs)
            return
        }
        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }

    private fun copy(clipboardArgs: ClipboardArgs) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(clipboardArgs.clipLabel, clipboardArgs.clipText))
        Toast.makeText(requireActivity(), clipboardArgs.toastMessage, Toast.LENGTH_LONG).show()
    }
}