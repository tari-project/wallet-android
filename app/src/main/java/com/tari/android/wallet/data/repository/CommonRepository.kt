package com.tari.android.wallet.data.repository

import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import io.reactivex.subjects.BehaviorSubject

open class CommonRepository(val networkRepository: NetworkRepository) {
    val updateNotifier =  BehaviorSubject.create<Unit>()
}