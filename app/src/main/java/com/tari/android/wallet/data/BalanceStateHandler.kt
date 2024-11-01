package com.tari.android.wallet.data

import com.tari.android.wallet.model.BalanceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceStateHandler @Inject constructor(){

    private val _balanceState = MutableStateFlow(BalanceInfo())
    val balanceState = _balanceState.asStateFlow()

    fun updateBalanceState(balanceInfo: BalanceInfo){
        _balanceState.update { balanceInfo }
    }
}