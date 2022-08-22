package com.tari.android.wallet.ui.component.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialog

abstract class CommonView<VM : CommonViewModel, VB : ViewBinding> : LinearLayout {
    lateinit var viewModel: VM

    lateinit var ui: VB
        private set

    private var currentDialog: TariDialog? = null

    abstract fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): VB

    lateinit var viewLifecycle: LifecycleOwner

    constructor(context: Context) : super(context, null) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        ui = bindingInflate(LayoutInflater.from(context), this, true)

        setup()
    }

    abstract fun setup()

    open fun bindViewModel(viewModel: VM) {
        this.viewModel = viewModel

        viewModel.openLink.observe(viewLifecycle) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        viewModel.modularDialog.observe(viewLifecycle) { replaceDialog(ModularDialog(context, it)) }
    }

    //todo extract to DialogManager or something
    protected fun replaceDialog(dialog: TariDialog) {
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
}