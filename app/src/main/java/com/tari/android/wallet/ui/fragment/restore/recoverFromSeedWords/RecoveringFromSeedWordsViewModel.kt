package com.tari.android.wallet.ui.fragment.restore.recoverFromSeedWords

import androidx.lifecycle.LiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordState

class RecoveringFromSeedWordsViewModel() : CommonViewModel() {
    init {
        component?.inject(this)
    }

    private val _state = SingleLiveEvent<EnterRestorationPasswordState>()
    val state: LiveData<EnterRestorationPasswordState> = _state

    private val _navigation = SingleLiveEvent<RecoveringFromSeedWordsNavigation>()
    val navigation: LiveData<RecoveringFromSeedWordsNavigation> = _navigation
}

