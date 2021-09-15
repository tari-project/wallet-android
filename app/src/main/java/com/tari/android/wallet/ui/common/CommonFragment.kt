package com.tari.android.wallet.ui.common

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.MutedBackPressedCallback
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog

abstract class CommonFragment<Binding : ViewBinding, VM : CommonViewModel> : Fragment() {

    private var currentDialog: TariDialog? = null

    protected var blockingBackPressDispatcher = MutedBackPressedCallback(false)

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

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

        observe(confirmDialog) { replaceDialog(ConfirmDialog(requireContext(), it)) }

        observe(errorDialog) { replaceDialog(ErrorDialog(requireContext(), it)) }

        observe(loadingDialog) { if (it.isShow) replaceDialog(TariProgressDialog(requireContext(), it)) else currentDialog?.dismiss() }

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
        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }
}