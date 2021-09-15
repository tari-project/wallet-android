package com.tari.android.wallet.ui.common

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.dialog.inProgress.TariProgressDialog

abstract class CommonActivity<Binding : ViewBinding, VM : CommonViewModel> : AppCompatActivity() {

    private var currentDialog: TariDialog? = null

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        this@CommonActivity.viewModel = viewModel

        observe(backPressed) { onBackPressed() }

        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        observe(confirmDialog) { replaceDialog(ConfirmDialog(this@CommonActivity, it)) }

        observe(errorDialog) { replaceDialog(ErrorDialog(this@CommonActivity, it)) }

        observe(loadingDialog) { if (it.isShow) replaceDialog(TariProgressDialog(this@CommonActivity, it)) else currentDialog?.dismiss() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun replaceDialog(dialog: TariDialog) {
        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }
}


