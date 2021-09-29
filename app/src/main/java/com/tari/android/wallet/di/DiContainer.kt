package com.tari.android.wallet.di

import com.tari.android.wallet.application.TariWalletApplication

object DiContainer {
    internal lateinit var appComponent: ApplicationComponent

    internal fun initContainer(app: TariWalletApplication) {
        appComponent = initDagger(app)
    }

    internal fun reinitContainer() {
        appComponent = initDagger(TariWalletApplication.INSTANCE.get()!!)
        TariWalletApplication.INSTANCE.get()!!.initApplication()
    }

    private fun initDagger(app: TariWalletApplication): ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(app))
            .build()
}