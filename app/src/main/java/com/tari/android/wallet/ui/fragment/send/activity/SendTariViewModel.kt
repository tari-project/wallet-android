package com.tari.android.wallet.ui.fragment.send.activity

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.yat.YatAdapter
import javax.inject.Inject

class SendTariViewModel : CommonViewModel() {
    @Inject
    lateinit var yatAdapter: YatAdapter

    init {
        component.inject(this)
    }
}