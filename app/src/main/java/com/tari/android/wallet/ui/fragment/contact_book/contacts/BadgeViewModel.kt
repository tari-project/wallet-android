package com.tari.android.wallet.ui.fragment.contact_book.contacts

import com.tari.android.wallet.ui.common.CommonViewModel

class BadgeViewModel : CommonViewModel() {
    var closeLastBadge: () -> Unit = {}

    fun openNew(closeAction: () -> Unit) {
        runCatching { closeLastBadge.invoke() }
        closeLastBadge = closeAction
    }
}