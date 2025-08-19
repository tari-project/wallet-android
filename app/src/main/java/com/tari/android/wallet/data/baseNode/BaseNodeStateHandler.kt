package com.tari.android.wallet.data.baseNode

import com.tari.android.wallet.model.TariBaseNodeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseNodeStateHandler @Inject constructor() {

    private val _walletScannedHeight = MutableStateFlow(0)
    val walletScannedHeight = _walletScannedHeight.asStateFlow()

    private val _baseNodeState = MutableStateFlow(TariBaseNodeState(heightOfLongestChain = BigInteger.ZERO))
    val baseNodeState = _baseNodeState.asStateFlow()

    fun saveBaseNodeState(baseNodeState: TariBaseNodeState) {
        _baseNodeState.update { baseNodeState }
    }

    fun saveWalletScannedHeight(height: Int) {
        _walletScannedHeight.update { height }
    }

    fun clear() {
        _baseNodeState.update { TariBaseNodeState(heightOfLongestChain = BigInteger.ZERO) }
        _walletScannedHeight.update { 0 }
    }
}