package com.tari.android.wallet.di

import com.tari.android.wallet.application.TariWalletApplication

object DiContainer {
    lateinit var appComponent: ApplicationComponent

    fun initContainer(app: TariWalletApplication) {
        appComponent = initDagger(app)
    }

    fun reInitContainer() {
        appComponent = initDagger(TariWalletApplication.INSTANCE.get()!!)
        TariWalletApplication.INSTANCE.get()!!.initApplication()
    }

    private fun initDagger(app: TariWalletApplication): ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(app))
            .build()
}