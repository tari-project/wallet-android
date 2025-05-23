package com.tari.android.wallet.ui.common

import androidx.viewbinding.ViewBinding

abstract class CommonXmlActivity<Binding : ViewBinding, VM : CommonViewModel> : CommonActivity<VM>() {

    lateinit var ui: Binding
}