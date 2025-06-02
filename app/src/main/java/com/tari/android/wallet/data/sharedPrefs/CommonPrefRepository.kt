package com.tari.android.wallet.data.sharedPrefs

import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import io.reactivex.subjects.BehaviorSubject

open class CommonPrefRepository(val networkRepository: NetworkPrefRepository) {
    val updateNotifier = BehaviorSubject.create<Unit>()
}