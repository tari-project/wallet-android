package com.tari.android.wallet.ui.extension

import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.ApplicationComponent

internal val FragmentActivity.appComponent: ApplicationComponent
    get() = (this.application as TariWalletApplication).appComponent
