package com.tari.android.wallet.ui.common.recyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

data class ViewHolderBuilder(val createView: (LayoutInflater, ViewGroup, Boolean) -> ViewBinding, val itemJavaClass : Class<*>, val createViewHolder: (ViewBinding) -> CommonViewHolder<*, *>)