package com.tari.android.wallet.data.repository

import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import io.reactivex.subjects.BehaviorSubject

open class CommonRepository(val networkRepository: NetworkPrefRepository) {
    val updateNotifier =  BehaviorSubject.create<Unit>()
}