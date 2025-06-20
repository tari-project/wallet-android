package com.tari.android.wallet.ui.screen.contactBook.obsolete.root.share

import com.tari.android.wallet.infrastructure.ShareType

class ShareOptionArgs(val type: ShareType, val title: String, val icon: Int, var isSelected: Boolean = false, val onClick: () -> Unit)


