package com.tari.android.wallet.ui.common.recyclerView

import java.io.Serializable

abstract class CommonViewHolderItem : Serializable {

    // override if need nice list refreshing
    abstract val viewHolderUUID: String

    override fun equals(other: Any?): Boolean = this === other

    open fun deepCopy(): CommonViewHolderItem = this

    override fun hashCode(): Int = super.hashCode()
}