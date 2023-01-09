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
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.mainList.MutedBackPressedCallback
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog

abstract class CommonFragment<Binding : ViewBinding, VM : CommonViewModel> : Fragment() {

    lateinit var clipboardManager: ClipboardManager

    private val dialogManager = DialogManager()

    protected var blockingBackPressDispatcher = MutedBackPressedCallback(false)

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

    var grantedAction: () -> Unit = {}
    var notGrantedAction: () -> Unit = {}

    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            grantedAction()
        } else {
            notGrantedAction()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        observe(loadingDialog) { dialogManager.handleProgress(it) }

        observe(dismissDialog) { dialogManager.dismiss() }

        observe(showToast) { TariToast(requireContext(), it) }

        observe(blockedBackPressed) {
            blockingBackPressDispatcher.isEnabled = it
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

    override fun onDestroy() {
        dialogManager.dismiss()
        super.onDestroy()
    }
}