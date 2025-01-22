package com.tari.android.wallet.ui.common

import androidx.viewbinding.ViewBinding

abstract class CommonXmlFragment<Binding : ViewBinding, VM : CommonViewModel> : CommonFragment<VM>() {

    protected lateinit var ui: Binding
}