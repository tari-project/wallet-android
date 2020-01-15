package com.tari.android.wallet.di

import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, WalletModule::class])
interface ApplicationComponent {

    fun inject(activity: BaseActivity)

    fun inject(service: WalletService)

}