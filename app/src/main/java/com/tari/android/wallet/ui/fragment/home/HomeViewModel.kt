package com.tari.android.wallet.ui.fragment.home

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.yat.YatAdapter
import javax.inject.Inject

class HomeViewModel: CommonViewModel() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    init {
        component.inject(this)
    }
}