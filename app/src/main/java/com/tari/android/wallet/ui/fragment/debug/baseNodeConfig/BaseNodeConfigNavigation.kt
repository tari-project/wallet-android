package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig

sealed class BaseNodeConfigNavigation {
    object ToAddCustomBaseNode : BaseNodeConfigNavigation()

    object ToChangeBaseNode : BaseNodeConfigNavigation()
}