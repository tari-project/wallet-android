package com.tari.android.wallet.ui.viewModel

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.extension.observe

abstract class CommonFragment<Binding: ViewBinding, VM: CommonViewModel> : Fragment() {

    protected lateinit var ui: Binding

    protected lateinit var viewModel: VM

    fun bindViewModel(viewModel: VM) = with(viewModel) {
        observe(openLink) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }
}