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
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.component.tari.toast.TariToast
import com.tari.android.wallet.ui.dialog.modular.ModularDialog

abstract class CommonView<VM : CommonViewModel, VB : ViewBinding> : LinearLayout {

    lateinit var viewModel: VM

    lateinit var ui: VB
        private set

    private val dialogManager = DialogManager()

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

        dialogManager.context = context
        setup()
    }

    abstract fun setup()

    open fun bindViewModel(viewModel: VM) {
        this.viewModel = viewModel

        viewModel.openLink.observe(viewLifecycle) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        viewModel.modularDialog.observe(viewLifecycle) { dialogManager.replace(ModularDialog(context, it)) }

        viewModel.dismissDialog.observe(viewLifecycle) { dialogManager.dismiss() }

        viewModel.loadingDialog.observe(viewLifecycle) { dialogManager.handleProgress(it) }

        viewModel.showToast.observe(viewLifecycle) { TariToast(context, it) }

        viewModel.navigation.observe(viewLifecycle) { viewModel.tariNavigator.navigate(it) }
    }
}